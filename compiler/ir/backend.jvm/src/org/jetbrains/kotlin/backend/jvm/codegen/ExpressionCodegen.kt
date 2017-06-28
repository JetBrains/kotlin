/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicFunction
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.inline.NameGenerator
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.inline.TypeParameterMappings
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_THROWABLE_TYPE
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

open class ExpressionInfo(val expression: IrExpression)

class LoopInfo(val loop: IrLoop, val continueLabel: Label, val breakLabel: Label): ExpressionInfo(loop)

class TryInfo(val tryBlock: IrTry) : ExpressionInfo(tryBlock) {
    val gaps = mutableListOf<Label>()
}

class BlockInfo private constructor(val parent: BlockInfo?) {
    val variables: MutableList<VariableInfo> = mutableListOf()

    private val infos = Stack<ExpressionInfo>()

    fun create() = BlockInfo(this).apply {
        this@apply.infos.addAll(this@BlockInfo.infos)
    }

    fun addInfo(loop: ExpressionInfo) {
        infos.add(loop)
    }

    fun removeInfo(info: ExpressionInfo) {
        assert (peek() == info)
        pop()
    }

    fun pop(): ExpressionInfo = infos.pop()

    fun peek(): ExpressionInfo = infos.peek()

    fun isEmpty(): Boolean = infos.isEmpty()

    fun hasFinallyBlocks(): Boolean = infos.firstIsInstanceOrNull<TryInfo>() != null

    companion object {
        fun create() = BlockInfo(null)
    }
}

class VariableInfo(val descriptor: VariableDescriptor, val index: Int, val type: Type, val startLabel: Label)

@Suppress("IMPLICIT_CAST_TO_ANY")
class ExpressionCodegen(
        val irFunction: IrFunction,
        val frame: FrameMap,
        val mv: InstructionAdapter,
        val classCodegen: ClassCodegen
) : IrElementVisitor<StackValue, BlockInfo>, BaseExpressionCodegen {

    /*TODO*/
    val intrinsics = IrIntrinsicMethods(classCodegen.context.irBuiltIns)

    val typeMapper = classCodegen.typeMapper

    val returnType = typeMapper.mapReturnType(irFunction.descriptor)

    val state = classCodegen.state

    fun generate() {
        mv.visitCode()
        val info = BlockInfo.create()
        val result = irFunction.body?.accept(this, info)
//        result?.apply {
//            coerce(this.type, irFunction.body!!.as)
//        }
        val returnType = typeMapper.mapReturnType(irFunction.descriptor)
        if (returnType == Type.VOID_TYPE) {
            //for implicit return
            mv.areturn(Type.VOID_TYPE)
        }
        writeLocalVariablesInTable(info)
        mv.visitEnd()
    }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): StackValue {
        return body.statements.fold(none()) {
            _, exp ->
            exp.accept(this, data)
        }
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): StackValue {
        val info = data.create()
        return super.visitBlock(expression, info).apply {
            if (!expression.isTransparentScope) {
                writeLocalVariablesInTable(info)
            }
            else {
                info.variables.forEach {
                    data.variables.add(it)
                }
            }
        }
    }

    private fun writeLocalVariablesInTable(info: BlockInfo) {
        val endLabel = markNewLabel()
        info.variables.forEach {
            mv.visitLocalVariable(it.descriptor.name.asString(), it.type.descriptor, null, it.startLabel, endLabel, it.index)
        }

        info.variables.reversed().forEach {
            frame.leave(it.descriptor)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo): StackValue {
        val result = expression.statements.fold(none()) {
            _, exp ->
            //coerceNotToUnit(r.type, Type.VOID_TYPE)
            exp.accept(this, data)
        }
        coerceNotToUnit(result.type, expression.asmType)
        return expression.onStack
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: BlockInfo): StackValue {
        return generateCall(expression, null, data)
    }

    override fun visitCall(expression: IrCall, data: BlockInfo): StackValue {
        if (expression.descriptor is ConstructorDescriptor) {
            return generateNewCall(expression, data)
        }
        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateNewCall(expression: IrCall, data: BlockInfo): StackValue {
        val type = expression.asmType
        if (type.sort == Type.ARRAY) {
            //noinspection ConstantConditions
            return generateNewArray(expression, data)
        }

        mv.anew(expression.asmType)
        mv.dup()
        generateCall(expression, expression.superQualifier, data)
        return expression.onStack
    }

    fun generateNewArray(
            expression: IrCall, data: BlockInfo
    ): StackValue {
        val args = expression.descriptor.valueParameters
        assert(args.size == 1 || args.size == 2) { "Unknown constructor called: " + args.size + " arguments" }

        if (args.size == 1) {
            val sizeExpression = expression.getValueArgument(0)!!
            gen(sizeExpression, Type.INT_TYPE, data)
            newArrayInstruction(expression.type)
            return expression.onStack
        }

        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrMemberAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): StackValue {
        val callable = resolveToCallable(expression, superQualifier != null)
        if (callable is IrIntrinsicFunction) {
            return callable.invoke(mv, this, data)
        } else {
            val callGenerator = getOrCreateCallGenerator(expression, expression.descriptor)

            val receiver = expression.dispatchReceiver
            receiver?.apply {
                //gen(receiver, callable.dispatchReceiverType!!, data)
                callGenerator.genValueAndPut(null, this, callable.dispatchReceiverType!!, -1, this@ExpressionCodegen, data)
            }

            expression.extensionReceiver?.apply {
                //gen(this, callable.extensionReceiverType!!, data)
                callGenerator.genValueAndPut(null, this, callable.extensionReceiverType!!, -1, this@ExpressionCodegen, data)
            }

            val defaultMask = DefaultCallArgs(callable.valueParameterTypes.size)
            expression.descriptor.valueParameters.forEachIndexed { i, parameterDescriptor ->
                val arg = expression.getValueArgument(i)
                val parameterType = callable.valueParameterTypes[i]
                when {
                    arg != null -> {
                        callGenerator.genValueAndPut(parameterDescriptor, arg, parameterType, i, this@ExpressionCodegen, data)
                    }
                    parameterDescriptor.hasDefaultValue() -> {
                        callGenerator.putValueIfNeeded(parameterType, StackValue.createDefaulValue(parameterType), ValueKind.DEFAULT_PARAMETER, i, this@ExpressionCodegen)
                        defaultMask.mark(i)
                    }
                    else -> {
                        assert(parameterDescriptor.varargElementType != null)
                        //empty vararg

                        callGenerator.putValueIfNeeded(
                                parameterType,
                                StackValue.operation(parameterType) {
                                    it.aconst(0)
                                    it.newarray(correctElementType(parameterType))
                                },
                                ValueKind.GENERAL_VARARG, i, this@ExpressionCodegen)
                    }
                }
            }

            callGenerator.genCall(
                    callable,
                    defaultMask.generateOnStackIfNeeded(callGenerator, expression.descriptor is ConstructorDescriptor, this),
                    this
            )

            val returnType = expression.descriptor.returnType
            if (returnType != null && KotlinBuiltIns.isNothing(returnType)) {
                mv.aconst(null)
                mv.athrow()
            }
            return StackValue.onStack(callable.returnType)
        }
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BlockInfo): StackValue {
        //HACK
        StackValue.local(0, OBJECT_TYPE).put(OBJECT_TYPE, mv)
        return super.visitDelegatingConstructorCall(expression, data)
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): StackValue {
        val varType = typeMapper.mapType(declaration.descriptor)
        val index = frame.enter(declaration.descriptor, varType)

        declaration.initializer?.apply {
            StackValue.local(index, varType).store(gen(this, varType, data), mv)
        }

        val info = VariableInfo(
                declaration.descriptor,
                index,
                varType,
                markNewLabel()
        )
        data.variables.add(info)

        return none()
    }

    fun gen(expression: IrElement, type: Type, data: BlockInfo): StackValue {
        gen(expression, data).put(type, mv)
        return onStack(type)
    }

    fun gen(expression: IrElement, data: BlockInfo): StackValue {
        return expression.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): StackValue {
        return generateLocal(expression.descriptor, expression.asmType)
    }

    private fun generateFieldValue(expression: IrFieldAccessExpression, data: BlockInfo): StackValue {
        val receiverValue = expression.receiver?.accept(this, data) ?: StackValue.none()
        val propertyDescriptor = expression.descriptor
        val fieldType = typeMapper.mapType(propertyDescriptor.type)
        val ownerType = typeMapper.mapImplementationOwner(propertyDescriptor)
        val fieldName = propertyDescriptor.name.asString()
        val isStatic = expression.receiver == null // TODO
        return StackValue.field(fieldType, ownerType, fieldName, isStatic, receiverValue, propertyDescriptor)
    }

    override fun visitGetField(expression: IrGetField, data: BlockInfo): StackValue {
        val value = generateFieldValue(expression, data)
        value.put(value.type, mv)
        return onStack(value.type)
    }

    override fun visitSetField(expression: IrSetField, data: BlockInfo): StackValue {
        val fieldValue = generateFieldValue(expression, data)
        fieldValue.store(expression.value.accept(this, data), mv)
        return none()
    }

    private fun generateLocal(descriptor: CallableDescriptor, type: Type): StackValue {
        val index = findLocalIndex(descriptor)
        StackValue.local(index, type).put(type, mv)
        return onStack(type)
    }

    private fun findLocalIndex(descriptor: CallableDescriptor): Int {
        val index = frame.getIndex(descriptor).apply {
            if (this < 0) throw AssertionError("Non-mapped local variable descriptor: $descriptor")
        }
        return index
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): StackValue {
        val value = expression.value.accept(this, data)
        StackValue.local(findLocalIndex(expression.descriptor), expression.descriptor.asmType).store(value, mv)
        return none()
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): StackValue {
        val value = expression.value
        val type = expression.asmType
        StackValue.constant(value, type).put(type, mv)
        return expression.onStack
    }

    override fun visitElement(element: IrElement, data: BlockInfo): StackValue {
        TODO("not implemented for $element") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitClass(declaration: IrClass, data: BlockInfo): StackValue {
        classCodegen.generateDeclaration(declaration)
        return none()
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: BlockInfo): StackValue {
        return none()
    }

    override fun visitVararg(expression: IrVararg, data: BlockInfo): StackValue {
        val outType = expression.type
        val type = expression.asmType
        assert(type.sort == Type.ARRAY)
        val elementType = correctElementType(type)
        val arguments = expression.elements
        val size = arguments.size

        val hasSpread = arguments.firstIsInstanceOrNull<IrSpreadElement>() != null

        if (hasSpread) {
            val arrayOfReferences = KotlinBuiltIns.isArray(outType)
            if (size == 1) {
                // Arrays.copyOf(receiverValue, newLength)
                val argument = (arguments[0] as IrSpreadElement).expression
                val arrayType = if (arrayOfReferences)
                    Type.getType("[Ljava/lang/Object;")
                else
                    Type.getType("[" + elementType.descriptor)
                gen(argument, type, data)
                mv.dup()
                mv.arraylength()
                mv.invokestatic("java/util/Arrays", "copyOf", Type.getMethodDescriptor(arrayType, arrayType, Type.INT_TYPE), false)
                if (arrayOfReferences) {
                    mv.checkcast(type)
                }
            }
            else {
                val owner: String
                val addDescriptor: String
                val toArrayDescriptor: String
                if (arrayOfReferences) {
                    owner = "kotlin/jvm/internal/SpreadBuilder"
                    addDescriptor = "(Ljava/lang/Object;)V"
                    toArrayDescriptor = "([Ljava/lang/Object;)[Ljava/lang/Object;"
                }
                else {
                    val spreadBuilderClassName = AsmUtil.asmPrimitiveTypeToLangPrimitiveType(elementType)!!.typeName.identifier + "SpreadBuilder"
                    owner = "kotlin/jvm/internal/" + spreadBuilderClassName
                    addDescriptor = "(" + elementType.descriptor + ")V"
                    toArrayDescriptor = "()" + type.descriptor
                }
                mv.anew(Type.getObjectType(owner))
                mv.dup()
                mv.iconst(size)
                mv.invokespecial(owner, "<init>", "(I)V", false)
                for (i in 0..size - 1) {
                    mv.dup()
                    val argument = arguments[i]
                    if (argument is IrSpreadElement) {
                        gen(argument.expression, OBJECT_TYPE, data)
                        mv.invokevirtual(owner, "addSpread", "(Ljava/lang/Object;)V", false)
                    }
                    else {
                        gen(argument, elementType, data)
                        mv.invokevirtual(owner, "add", addDescriptor, false)
                    }
                }
                if (arrayOfReferences) {
                    mv.dup()
                    mv.invokevirtual(owner, "size", "()I", false)
                    newArrayInstruction(outType)
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                    mv.checkcast(type)
                }
                else {
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                }
            }
        }
        else {
            mv.iconst(size)
            val asmType = elementType
            newArrayInstruction(expression.type)
            for ((i, element)  in expression.elements.withIndex()) {
                mv.dup()
                StackValue.constant(i, Type.INT_TYPE).put(Type.INT_TYPE, mv)
                val rightSide = gen(element, asmType, data)
                StackValue.arrayElement(asmType, StackValue.onStack(asmType), StackValue.onStack(Type.INT_TYPE)).store(rightSide, mv)
            }
        }
        return expression.onStack
    }

    fun newArrayInstruction(arrayType: KotlinType) {
        if (KotlinBuiltIns.isArray(arrayType)) {
            val elementJetType = arrayType.arguments[0].type
//            putReifiedOperationMarkerIfTypeIsReifiedParameter(
//                    elementJetType,
//                    ReifiedTypeInliner.OperationKind.NEW_ARRAY
//            )
            mv.newarray(boxType(elementJetType.asmType))
        }
        else {
            val type = typeMapper.mapType(arrayType)
            mv.newarray(correctElementType(type))
        }
    }


    fun markNewLabel(): Label {
        return Label().apply { mv.visitLabel(this) }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): StackValue {
        val value = expression.value.apply {
            gen(this, returnType, data)
        }

        val afterReturnLabel = Label()
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)

        mv.areturn(returnType)
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return expression.onStack
    }


    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
        val resultType = expression.asmType
        genIfWithBranches(expression.branches[0], data, resultType, expression.branches.drop(1))
        return expression.onStack
    }


    fun genIfWithBranches(branch: IrBranch, data: BlockInfo, type: Type, otherBranches: List<IrBranch>) {
        val elseLabel = Label()
        val condition = branch.condition
        val thenBranch = branch.result
        //TODO don't generate condition for else branch - java verifier fails with empty stack
        val elseBranch = branch is IrElseBranch
        if (!elseBranch) {
            gen(condition, data)
            BranchedValue.condJump(StackValue.onStack(condition.asmType), elseLabel, true, mv)
        }

        val end = Label()

        thenBranch.apply {
            gen(this, type, data)
            //coerceNotToUnit(this.asmType, type)
        }

        mv.goTo(end)
        mv.mark(elseLabel)

        if (!otherBranches.isEmpty()) {
            val nextBranch = otherBranches.first()
            genIfWithBranches(nextBranch, data, type, otherBranches.drop(1))
        }

        mv.mark(end)
    }


    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        val asmType = expression.typeOperand.asmType
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.accept(this, data)
                coerce(expression.argument.asmType, Type.VOID_TYPE, mv)
                return none()
            }

            IrTypeOperator.IMPLICIT_CAST -> {
                gen(expression.argument, asmType, data)
            }

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                val value = expression.argument.accept(this, data)
                value.put(boxType(value.type), mv)
                if (value.type === Type.VOID_TYPE) {
                    StackValue.putUnitInstance(mv)
                }
                val boxedType = boxType(asmType)
                generateAsCast(mv, expression.typeOperand, boxedType, expression.operator == IrTypeOperator.SAFE_CAST)
                return onStack(boxedType)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> {
                gen(expression.argument, OBJECT_TYPE, data)
                val type = boxType(asmType)
                generateIsCheck(mv, expression.typeOperand, type)
                if (IrTypeOperator.NOT_INSTANCEOF == expression.operator) {
                    StackValue.not(StackValue.onStack(Type.BOOLEAN_TYPE)).put(Type.BOOLEAN_TYPE, mv)
                }
            }

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                gen(expression.argument, OBJECT_TYPE, data)
                mv.dup()
                mv.visitLdcInsn("TODO provide message") /*TODO*/
                mv.invokestatic("kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                               "(Ljava/lang/Object;Ljava/lang/String;)V", false)
            }

            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> {
                gen(expression.argument, Type.INT_TYPE, data)
                StackValue.coerce(Type.INT_TYPE, typeMapper.mapType(expression.type), mv)
            }
        }
        return expression.onStack
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: BlockInfo): StackValue {
        AsmUtil.genStringBuilderConstructor(mv)
        expression.arguments.forEach {
            AsmUtil.genInvokeAppendMethod(mv, gen(it, data).type)
        }

        mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        return expression.onStack
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: BlockInfo): StackValue {
        val continueLabel = markNewLabel()
        val endLabel = Label()
        val condition = loop.condition
        gen(condition, data)
        BranchedValue.condJump(StackValue.onStack(condition.asmType), endLabel, true, mv)

        with(LoopInfo(loop, continueLabel, endLabel)) {
            data.addInfo(this)
            loop.body?.apply {
                gen(this, data)
            }
            data.removeInfo(this)
        }
        mv.goTo(continueLabel)
        mv.mark(endLabel)

        return loop.onStack
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: BlockInfo): StackValue {
        generateBreakOrContinueExpression(jump, Label(), data)
        return none()
    }

    private fun generateBreakOrContinueExpression(
            expression: IrBreakContinue,
            afterBreakContinueLabel: Label,
            data: BlockInfo
    ) {
        if (data.isEmpty()) {
            throw UnsupportedOperationException("Target label for break/continue not found")
        }

        val stackElement = data.peek()

        when (stackElement) {
            is TryInfo -> //noinspection ConstantConditions
                genFinallyBlockOrGoto(stackElement, null, afterBreakContinueLabel, data)
            is LoopInfo -> {
                val loop = expression.loop
                //noinspection ConstantConditions
                if (loop == stackElement.loop) {
                    val label = if (expression is IrBreak) stackElement.breakLabel else stackElement.continueLabel
                    mv.fixStackAndJump(label)
                    mv.mark(afterBreakContinueLabel)
                    return
                }
            }
            else -> throw UnsupportedOperationException("Wrong BlockStackElement in processing stack")
        }

        data.pop()
        val result = generateBreakOrContinueExpression(expression, afterBreakContinueLabel, data)
        data.addInfo(stackElement)
        return result
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BlockInfo): StackValue {
        val entry = markNewLabel()
        val endLabel = Label()
        val continueLabel = Label()

        with(LoopInfo(loop, continueLabel, endLabel)) {
            data.addInfo(this)
            loop.body?.apply {
                gen(this, data)
            }
            data.removeInfo(this)
        }

        mv.visitLabel(continueLabel)
        val condition = loop.condition
        gen(condition, data)
        BranchedValue.condJump(StackValue.onStack(condition.asmType), entry, false, mv)
        mv.mark(endLabel)

        return loop.onStack
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): StackValue {
        val finallyExpression = aTry.finallyExpression
        val tryInfo = if (finallyExpression != null) TryInfo(aTry) else null
        if (tryInfo != null) {
            data.addInfo(tryInfo)
        }

        val tryBlockStart = markNewLabel()
        mv.nop()
        gen(aTry.tryResult, aTry.asmType, data)
        val tryBlockEnd = markNewLabel()

        val tryRegions = getCurrentTryIntervals(tryInfo, tryBlockStart, tryBlockEnd)

        val tryCatchBlockEnd = Label()
        genFinallyBlockOrGoto(tryInfo,  tryCatchBlockEnd, null, data)

        val catches = aTry.catches
        for (clause in catches) {
            val clauseStart = markNewLabel()
            val descriptor = clause.parameter
            val descriptorType = descriptor.asmType
            val index = frame.enter(descriptor, descriptorType)
            mv.store(index, descriptorType)

            val catchBody = clause.result
            gen(catchBody, catchBody.asmType, data)

            frame.leave(descriptor)

            val clauseEnd = markNewLabel()

            mv.visitLocalVariable(descriptor.name.asString(), descriptorType.descriptor, null, clauseStart, clauseEnd,
                                 index)

            genFinallyBlockOrGoto(tryInfo, if (clause != catches.last() || finallyExpression != null) tryCatchBlockEnd else null, null, data)

            generateExceptionTable(clauseStart, tryRegions, descriptorType.internalName)
        }

        //for default catch clause
        if (finallyExpression != null) {
            val defaultCatchStart = Label()
            mv.mark(defaultCatchStart)
            val savedException = frame.enterTemp(JAVA_THROWABLE_TYPE)
            mv.store(savedException, JAVA_THROWABLE_TYPE)

            val defaultCatchEnd = Label()
            mv.mark(defaultCatchEnd)

            //do it before finally block generation
            //javac also generates entry in exception table for default catch clause too!!!! so defaultCatchEnd as end parameter
            val defaultCatchRegions = getCurrentTryIntervals(tryInfo, tryBlockStart, defaultCatchEnd)

            genFinallyBlockOrGoto(tryInfo, null, null, data)

            mv.load(savedException, JAVA_THROWABLE_TYPE)
            frame.leaveTemp(JAVA_THROWABLE_TYPE)

            mv.athrow()

            generateExceptionTable(defaultCatchStart, defaultCatchRegions, null)
        }

        mv.mark(tryCatchBlockEnd)
        if (tryInfo != null) {
            data.removeInfo(tryInfo)
        }

        return aTry.onStack
    }

    private fun getCurrentTryIntervals(
            finallyBlockStackElement: TryInfo?,
            blockStart: Label,
            blockEnd: Label
    ): List<Label> {
        val gapsInBlock = if (finallyBlockStackElement != null) ArrayList<Label>(finallyBlockStackElement.gaps) else emptyList<Label>()
        assert(gapsInBlock.size % 2 == 0)
        val blockRegions = ArrayList<Label>(gapsInBlock.size + 2)
        blockRegions.add(blockStart)
        blockRegions.addAll(gapsInBlock)
        blockRegions.add(blockEnd)
        return blockRegions
    }

    private fun generateExceptionTable(catchStart: Label, catchedRegions: List<Label>, exception: String?) {
        var i = 0
        while (i < catchedRegions.size) {
            val startRegion = catchedRegions[i]
            val endRegion = catchedRegions[i + 1]
            mv.visitTryCatchBlock(startRegion, endRegion, catchStart, exception)
            i += 2
        }
    }

    private fun genFinallyBlockOrGoto(
            tryInfo: TryInfo?,
            tryCatchBlockEnd: Label?,
            afterJumpLabel: Label?,
            data: BlockInfo
    ) {
        if (tryInfo != null) {
            assert(tryInfo.gaps.size % 2 == 0) { "Finally block gaps are inconsistent" }

            val topOfStack = data.pop()
            assert(topOfStack === tryInfo) { "Top element of stack doesn't equals processing finally block" }

            val tryBlock = tryInfo.tryBlock
            val finallyStart = markNewLabel()
            tryInfo.gaps.add(finallyStart)

            //noinspection ConstantConditions
            gen(tryBlock.finallyExpression!!, Type.VOID_TYPE, data)
        }

        if (tryCatchBlockEnd != null) {
            mv.goTo(tryCatchBlockEnd)
        }

        if (tryInfo != null) {
            val finallyEnd = afterJumpLabel ?: Label()
            if (afterJumpLabel == null) {
                mv.mark(finallyEnd)
            }
            tryInfo.gaps.add(finallyEnd)

            data.addInfo(tryInfo)
        }
    }

    fun generateFinallyBlocksIfNeeded(returnType: Type, afterReturnLabel: Label, data: BlockInfo) {
        if (data.hasFinallyBlocks()) {
            if (Type.VOID_TYPE != returnType) {
                val returnValIndex = frame.enterTemp(returnType)
                val localForReturnValue = StackValue.local(returnValIndex, returnType)
                localForReturnValue.store(StackValue.onStack(returnType), mv)
                doFinallyOnReturn(afterReturnLabel, data)
                localForReturnValue.put(returnType, mv)
                frame.leaveTemp(returnType)
            }
            else {
                doFinallyOnReturn(afterReturnLabel, data)
            }
        }
    }


    private fun doFinallyOnReturn(afterReturnLabel: Label, data: BlockInfo) {
        if (!data.isEmpty()) {
            val stackElement = data.peek()
            when (stackElement) {
                is TryInfo -> genFinallyBlockOrGoto(stackElement, null, afterReturnLabel, data)
                is LoopInfo -> {

                }
                else -> throw UnsupportedOperationException("Wrong BlockStackElement in processing stack")
            }

            data.pop()
            doFinallyOnReturn(afterReturnLabel, data)
            data.addInfo(stackElement)
        }
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): StackValue {
        gen(expression.value, JAVA_THROWABLE_TYPE, data)
        mv.athrow()
        return expression.onStack
    }

    override fun visitClassReference(expression: IrClassReference, data: BlockInfo): StackValue {
        generateClassLiteralReference(expression, true, data)
        return expression.onStack
    }

    fun generateClassLiteralReference(
            receiverExpression: IrExpression,
            wrapIntoKClass: Boolean,
            data: BlockInfo
    ) {
        if (receiverExpression !is IrClassReference /* && DescriptorUtils.isObjectQualifier(receiverExpression.descriptor)*/) {
            JavaClassProperty.generateImpl(mv, gen(receiverExpression, data))
        }
        else {
//                if (TypeUtils.isTypeParameter(type)) {
//                    assert(TypeUtils.isReifiedTypeParameter(type)) { "Non-reified type parameter under ::class should be rejected by type checker: " + type }
//                    putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.JAVA_CLASS)
//                }

            putJavaLangClassInstance(mv, typeMapper.mapType(receiverExpression.descriptor.defaultType))
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }

    }

    private fun coerceNotToUnit(fromType: Type, toType: Type) {
        if (toType != AsmTypes.UNIT_TYPE) {
            coerce(fromType, toType, mv)
        }
    }

    val IrExpression.asmType: Type
        get() = typeMapper.mapType(this.type)

    val IrExpression.onStack: StackValue
        get() = StackValue.onStack(this.asmType)

    private fun resolveToCallable(irCall: IrMemberAccessExpression, isSuper: Boolean): Callable {
        val intrinsic = intrinsics.getIntrinsic(irCall.descriptor.original as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(irCall, typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor), classCodegen.context)
        }

        var descriptor = irCall.descriptor
        if (descriptor is TypeAliasConstructorDescriptor) {
            //TODO where is best to unwrap?
            descriptor = descriptor.underlyingConstructorDescriptor
        }
        if (descriptor is PropertyDescriptor) {
            descriptor = descriptor.getter!!
        }
        if (descriptor is CallableMemberDescriptor && JvmCodegenUtil.getDirectMember(descriptor) is SyntheticJavaPropertyDescriptor) {
            val propertyDescriptor = JvmCodegenUtil.getDirectMember(descriptor) as SyntheticJavaPropertyDescriptor
            if (descriptor is PropertyGetterDescriptor) {
                descriptor = propertyDescriptor.getMethod
            }
            else {
                descriptor = propertyDescriptor.setMethod!!
            }
        }
        return typeMapper.mapToCallableMethod(descriptor as FunctionDescriptor, isSuper)
    }

    private val KotlinType.asmType: Type
        get() = typeMapper.mapType(this)

    private val CallableDescriptor.asmType: Type
        get() = typeMapper.mapType(this)


    private fun getOrCreateCallGenerator(
            descriptor: CallableDescriptor,
            element: IrMemberAccessExpression?,
            typeParameterMappings: TypeParameterMappings?,
            isDefaultCompilation: Boolean
    ): IrCallGenerator {
        if (element == null) return IrCallGenerator.DefaultCallGenerator

        // We should inline callable containing reified type parameters even if inline is disabled
        // because they may contain something to reify and straight call will probably fail at runtime
        val isInline = (!state.isInlineDisabled || InlineUtil.containsReifiedTypeParameters(descriptor)) && (InlineUtil.isInline(descriptor) || InlineUtil.isArrayConstructorWithLambda(descriptor))

        if (!isInline) return IrCallGenerator.DefaultCallGenerator

        val original = unwrapInitialSignatureDescriptor(DescriptorUtils.unwrapFakeOverride(descriptor.original as FunctionDescriptor))
        if (isDefaultCompilation) {
            return TODO()
        }
        else {
            return IrInlineCodegen(this, state, original, typeParameterMappings!!, IrSourceCompilerForInline(state, element))
        }
    }

    internal fun getOrCreateCallGenerator(memberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor): IrCallGenerator {
        val typeArguments = descriptor.typeParameters.keysToMap { memberAccessExpression.getTypeArgumentOrDefault(it) }

        val mappings = TypeParameterMappings()
        for (entry in typeArguments.entries) {
            val key = entry.key
            val type = entry.value

            val isReified = key.isReified || InlineUtil.isArrayConstructorWithLambda(descriptor)

            val typeParameterAndReificationArgument = extractReificationArgument(type)
            if (typeParameterAndReificationArgument == null) {
                val approximatedType = approximateCapturedTypes(entry.value).upper
                // type is not generic
                val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
                val asmType = typeMapper.mapTypeParameter(approximatedType, signatureWriter)

                mappings.addParameterMappingToType(
                        key.name.identifier, approximatedType, asmType, signatureWriter.toString(), isReified
                )
            }
            else {
                mappings.addParameterMappingForFurtherReification(
                        key.name.identifier, type, typeParameterAndReificationArgument.second, isReified
                )
            }
        }

        return getOrCreateCallGenerator(descriptor, memberAccessExpression, mappings, false)
    }

    override val frameMap: FrameMap
        get() = frame
    override val visitor: InstructionAdapter
        get() = mv
    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo")

    override val lastLineNumber: Int
        get() = -1 //TODO

    override fun consumeReifiedOperationMarker(typeParameterDescriptor: TypeParameterDescriptor) {
        //TODO
    }

    override fun propagateChildReifiedTypeParametersUsages(reifiedTypeParametersUsages: ReifiedTypeParametersUsages) {
        //TODO
    }

    override fun pushClosureOnStack(classDescriptor: ClassDescriptor, putThis: Boolean, callGenerator: CallGenerator, functionReferenceReceiver: StackValue?) {
        //TODO
    }

    override fun markLineNumberAfterInlineIfNeeded() {
        //TODO
    }
}

private class DefaultArg(val index: Int)

private class Vararg(val index: Int)


fun DefaultCallArgs.generateOnStackIfNeeded(callGenerator: IrCallGenerator, isConstructor: Boolean, codegen: ExpressionCodegen): Boolean {
    val toInts = toInts()
    if (!toInts.isEmpty()) {
        for (mask in toInts) {
            callGenerator.putValueIfNeeded(Type.INT_TYPE, StackValue.constant(mask, Type.INT_TYPE), ValueKind.DEFAULT_MASK, -1, codegen)
        }

        val parameterType = if (isConstructor) AsmTypes.DEFAULT_CONSTRUCTOR_MARKER else AsmTypes.OBJECT_TYPE
        callGenerator.putValueIfNeeded(parameterType, StackValue.constant(null, parameterType), ValueKind.METHOD_HANDLE_IN_DEFAULT, -1, codegen)
    }
    return toInts.isNotEmpty()
}