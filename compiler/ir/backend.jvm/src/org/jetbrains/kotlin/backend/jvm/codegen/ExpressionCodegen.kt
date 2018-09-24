/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicFunction
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.lower.CrIrType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.inline.NameGenerator
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.inline.TypeParameterMappings
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_THROWABLE_TYPE
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

open class ExpressionInfo(val expression: IrExpression)

class LoopInfo(val loop: IrLoop, val continueLabel: Label, val breakLabel: Label) : ExpressionInfo(loop)

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
        assert(peek() == info)
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

class VariableInfo(val declaration: IrVariable, val index: Int, val type: Type, val startLabel: Label)

@Suppress("IMPLICIT_CAST_TO_ANY")
class ExpressionCodegen(
    val irFunction: IrFunction,
    val frame: IrFrameMap,
    val mv: InstructionAdapter,
    val classCodegen: ClassCodegen
) : IrElementVisitor<StackValue, BlockInfo>, BaseExpressionCodegen {

    /*TODO*/
    val intrinsics = IrIntrinsicMethods(classCodegen.context.irBuiltIns)

    val typeMapper = classCodegen.typeMapper

    val returnType = typeMapper.mapReturnType(irFunction.descriptor)

    val state = classCodegen.state

    private val sourceManager = classCodegen.context.psiSourceManager

    private val fileEntry = sourceManager.getFileEntry(irFunction.fileParent)

    fun generate() {
        mv.visitCode()
        val startLabel = markNewLabel()
        val info = BlockInfo.create()
        val result = irFunction.body!!.accept(this, info)
        markFunctionLineNumber()
        val returnType = typeMapper.mapReturnType(irFunction.descriptor)
        if (irFunction.body is IrExpressionBody) {
            mv.areturn(returnType)
            //TODO merge branch inside next one
        } else if (!endsWithReturn(irFunction.body!!)) {
            if (returnType == Type.VOID_TYPE) {
                mv.areturn(returnType)
            } else {
                StackValue.none().put(returnType, null, mv)
                mv.areturn(returnType)
            }
        }
        writeLocalVariablesInTable(info)
        writeParameterInLocalVariableTable(startLabel)
        mv.visitEnd()
    }

    private fun markFunctionLineNumber() {
        if (irFunction.origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER) {
            return
        }
        if (irFunction is IrConstructor && irFunction.isPrimary) {
            irFunction.markLineNumber(startOffset = true)
            return
        }
        val lastElement = irFunction.body!!.getLastElement()
        if (lastElement !is IrReturn) {
            irFunction.markLineNumber(startOffset = false)
        }
    }

    private fun IrElement.getLastElement(): IrElement {
        return when (this) {
            is IrStatementContainer -> if (this.statements.isEmpty()) this else this.statements[this.statements.size - 1].getLastElement()
            is IrExpressionBody -> this.expression.getLastElement()
            else -> this
        }
    }

    private fun writeParameterInLocalVariableTable(startLabel: Label) {
        if (!irFunction.isStatic) {
            mv.visitLocalVariable("this", classCodegen.type.descriptor, null, startLabel, markNewLabel(), 0)
        }
        val extensionReceiverParameter = irFunction.extensionReceiverParameter
        if (extensionReceiverParameter != null) {
            writeValueParameterInLocalVariableTable(extensionReceiverParameter, startLabel)
        }
        for (param in irFunction.valueParameters) {
            writeValueParameterInLocalVariableTable(param, startLabel)
        }
    }

    private fun writeValueParameterInLocalVariableTable(param: IrValueParameter, startLabel: Label) {
        val descriptor = param.descriptor
        val nameForDestructuredParameter = if (descriptor is ValueParameterDescriptor)
            ValueParameterDescriptorImpl.getNameForDestructuredParameterOrNull(descriptor) else null
        val type = typeMapper.mapType(descriptor)
        // NOTE: we expect all value parameters to be present in the frame.
        mv.visitLocalVariable(
            nameForDestructuredParameter ?: param.name.asString(),
            type.descriptor, null, startLabel, markNewLabel(), findLocalIndex(param.symbol)
        )
    }

    private fun endsWithReturn(body: IrBody): Boolean {
        val lastStatement = if (body is IrStatementContainer) {
            body.statements.lastOrNull() ?: body
        } else body
        return lastStatement is IrReturn
    }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): StackValue {
        return body.statements.fold(none()) { _, exp ->
            exp.accept(this, data).also {
                (exp as? IrExpression)?.markEndOfStatementIfNeeded()
            }
        }
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): StackValue {
        val info = data.create()
        return super.visitBlock(expression, info).apply {
            if (!expression.isTransparentScope) {
                writeLocalVariablesInTable(info)
            } else {
                info.variables.forEach {
                    data.variables.add(it)
                }
            }
        }
    }

    private fun writeLocalVariablesInTable(info: BlockInfo) {
        val endLabel = markNewLabel()
        info.variables.forEach {
            when (it.declaration.origin) {
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrDeclarationOrigin.FOR_LOOP_ITERATOR,
                IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE -> {
                    // Ignore implicitly created variables
                }
                else -> {
                    mv.visitLocalVariable(
                        it.declaration.name.asString(), it.type.descriptor, null, it.startLabel, endLabel, it.index
                    )
                }
            }
        }

        info.variables.reversed().forEach {
            frame.leave(it.declaration.symbol)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo): StackValue {
        val result = expression.statements.fold(none()) { _, exp ->
            //coerceNotToUnit(r.type, Type.VOID_TYPE)
            exp.accept(this, data)
        }
        return coerceNotToUnit(result.type, result.kotlinType, expression.type.toKotlinType())
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return generateCall(expression, null, data)
    }

    override fun visitCall(expression: IrCall, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
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
            newArrayInstruction(expression.type.toKotlinType())
            return expression.onStack
        }

        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrMemberAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): StackValue {
        val isSuperCall = superQualifier != null
        val callable = resolveToCallable(expression, isSuperCall)
        return if (callable is IrIntrinsicFunction) {
            callable.invoke(mv, this, data)
        } else {
            generateCall(expression, callable, data, isSuperCall)
        }
    }

    fun generateCall(expression: IrMemberAccessExpression, callable: Callable, data: BlockInfo, isSuperCall: Boolean = false): StackValue {
        val callGenerator = getOrCreateCallGenerator(expression, expression.descriptor)

        val receiver = expression.dispatchReceiver
        receiver?.apply {
            callGenerator.genValueAndPut(
                null, this,
                if (isSuperCall) receiver.asmType else callable.dispatchReceiverType!!,
                -1, this@ExpressionCodegen, data
            )
        }

        expression.extensionReceiver?.apply {
            callGenerator.genValueAndPut(null, this, callable.extensionReceiverType!!, -1, this@ExpressionCodegen, data)
        }

        callGenerator.beforeValueParametersStart()
        val defaultMask = DefaultCallArgs(callable.valueParameterTypes.size)
        val extraArgsShift =
            when {
                expression.descriptor is ConstructorDescriptor && isEnumClass(expression.descriptor.containingDeclaration) -> 2
                expression.descriptor is ConstructorDescriptor &&
                        (expression.descriptor.containingDeclaration as ClassDescriptor).isInner -> 1 // skip the `$outer` parameter
                else -> 0
            }
        expression.descriptor.valueParameters.forEachIndexed { i, parameterDescriptor ->
            val arg = expression.getValueArgument(i)
            val parameterType = callable.valueParameterTypes[i]
            when {
                arg != null -> {
                    callGenerator.genValueAndPut(parameterDescriptor, arg, parameterType, i, this@ExpressionCodegen, data)
                }
                parameterDescriptor.hasDefaultValue() -> {
                    callGenerator.putValueIfNeeded(
                        parameterType,
                        StackValue.createDefaultValue(parameterType),
                        ValueKind.DEFAULT_PARAMETER,
                        i,
                        this@ExpressionCodegen
                    )
                    defaultMask.mark(i - extraArgsShift/*TODO switch to separate lower*/)
                }
                else -> {
                    assert(parameterDescriptor.varargElementType != null)
                    //empty vararg

                    // Upper bound for type of vararg parameter should always have a form of 'Array<out T>',
                    // while its lower bound may be Nothing-typed after approximation
                    val type = typeMapper.mapType(parameterDescriptor.type.upperIfFlexible())
                    callGenerator.putValueIfNeeded(
                        parameterType,
                        StackValue.operation(type) {
                            it.aconst(0)
                            it.newarray(correctElementType(type))
                        },
                        ValueKind.GENERAL_VARARG, i, this@ExpressionCodegen
                    )
                }
            }
        }

        callGenerator.genCall(
            callable,
            defaultMask.generateOnStackIfNeeded(callGenerator, expression.descriptor is ConstructorDescriptor, this),
            this,
            expression
        )

        val returnType = expression.descriptor.returnType
        if (returnType != null && KotlinBuiltIns.isNothing(returnType)) {
            mv.aconst(null)
            mv.athrow()
        } else if (expression.descriptor !is ConstructorDescriptor) {
            return returnType?.run { coerceNotToUnit(callable.returnType, returnType, this) } ?: onStack(callable.returnType, returnType)
        } else {
            return none()
        }

        return expression.onStack
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
        val index = frame.enter(declaration.symbol, varType)

        declaration.markLineNumber(startOffset = true)

        declaration.initializer?.apply {
            StackValue.local(index, varType).store(gen(this, varType, data), mv)
            this.markLineNumber(startOffset = true)
        }

        val info = VariableInfo(
            declaration,
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
        expression.markLineNumber(startOffset = true)
        return generateLocal(expression.symbol, expression.asmType)
    }

    private fun generateFieldValue(expression: IrFieldAccessExpression, data: BlockInfo): StackValue {
        val receiverValue = expression.receiver?.accept(this, data) ?: StackValue.none()
        val propertyDescriptor = expression.descriptor

        val realDescriptor = DescriptorUtils.unwrapFakeOverride(propertyDescriptor)
        val fieldKotlinType = realDescriptor.original.type
        val fieldType = typeMapper.mapType(fieldKotlinType)
        val ownerType = typeMapper.mapImplementationOwner(propertyDescriptor)
        val fieldName = propertyDescriptor.name.asString()
        val isStatic = expression.receiver == null // TODO

        return StackValue.field(fieldType, fieldKotlinType, ownerType, fieldName, isStatic, receiverValue, realDescriptor)
    }

    override fun visitGetField(expression: IrGetField, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        val value = generateFieldValue(expression, data)
        value.put(value.type, mv)
        return onStack(value.type)
    }

    override fun visitSetField(expression: IrSetField, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        val fieldValue = generateFieldValue(expression, data)
        fieldValue.store(expression.value.accept(this, data), mv)
        return none()
    }

    private fun generateLocal(symbol: IrSymbol, type: Type): StackValue {
        val index = findLocalIndex(symbol)
        StackValue.local(index, type).put(type, mv)
        return onStack(type)
    }

    private fun findLocalIndex(irSymbol: IrSymbol): Int {
        return frame.getIndex(irSymbol).apply {
            if (this < 0) {
                if (irFunction.dispatchReceiverParameter != null) {
                    (irFunction.parent as? IrClass)?.takeIf { it.thisReceiver?.symbol == irSymbol }?.let { return 0 }
                }
                throw AssertionError(
                    "Non-mapped local declaration: " +
                            "${if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor} \n in ${irFunction.dump()}"
                )
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        val value = expression.value.accept(this, data)
        StackValue.local(findLocalIndex(expression.symbol), expression.descriptor.asmType).store(value, mv)
        return none()
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        val value = expression.value
        val type = expression.asmType
        StackValue.constant(value, type).put(type, mv)
        return expression.onStack
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: BlockInfo): StackValue {
        return body.expression.accept(this, data)
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
        expression.markLineNumber(startOffset = true)
        val outType = expression.type
        val type = expression.asmType
        assert(type.sort == Type.ARRAY)
        val elementType = correctElementType(type)
        val arguments = expression.elements
        val size = arguments.size

        val hasSpread = arguments.firstIsInstanceOrNull<IrSpreadElement>() != null

        if (hasSpread) {
            val arrayOfReferences = KotlinBuiltIns.isArray(outType.toKotlinType()!!)
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
            } else {
                val owner: String
                val addDescriptor: String
                val toArrayDescriptor: String
                if (arrayOfReferences) {
                    owner = "kotlin/jvm/internal/SpreadBuilder"
                    addDescriptor = "(Ljava/lang/Object;)V"
                    toArrayDescriptor = "([Ljava/lang/Object;)[Ljava/lang/Object;"
                } else {
                    val spreadBuilderClassName =
                        AsmUtil.asmPrimitiveTypeToLangPrimitiveType(elementType)!!.typeName.identifier + "SpreadBuilder"
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
                    } else {
                        gen(argument, elementType, data)
                        mv.invokevirtual(owner, "add", addDescriptor, false)
                    }
                }
                if (arrayOfReferences) {
                    mv.dup()
                    mv.invokevirtual(owner, "size", "()I", false)
                    newArrayInstruction(outType.toKotlinType())
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                    mv.checkcast(type)
                } else {
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                }
            }
        } else {
            mv.iconst(size)
            newArrayInstruction(expression.type.toKotlinType())
            val elementKotlinType = classCodegen.context.builtIns.getArrayElementType(outType.toKotlinType()!!)
            for ((i, element) in expression.elements.withIndex()) {
                mv.dup()
                StackValue.constant(i).put(Type.INT_TYPE, mv)
                val rightSide = gen(element, elementType, data)
                StackValue
                    .arrayElement(
                        elementType,
                        elementKotlinType,
                        StackValue.onStack(elementType, outType.toKotlinType()),
                        StackValue.onStack(Type.INT_TYPE)
                    )
                    .store(rightSide, mv)
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
        } else {
            val type = typeMapper.mapType(arrayType)
            mv.newarray(correctElementType(type))
        }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): StackValue {
        val value = expression.value.apply {
            gen(this, returnType, data)
        }

        val afterReturnLabel = Label()
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)

        expression.markLineNumber(startOffset = true)
        mv.areturn(returnType)
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return expression.onStack
    }


    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return genIfWithBranches(expression.branches[0], data, expression.type.toKotlinType(), expression.branches.drop(1))
    }


    private fun genIfWithBranches(branch: IrBranch, data: BlockInfo, type: KotlinType, otherBranches: List<IrBranch>): StackValue {
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

        val result = thenBranch.run {
            val stackValue = gen(this, data)
            coerceNotToUnit(stackValue.type, stackValue.kotlinType, type)
        }

        mv.goTo(end)
        mv.mark(elseLabel)

        if (!otherBranches.isEmpty()) {
            val nextBranch = otherBranches.first()
            genIfWithBranches(nextBranch, data, type, otherBranches.drop(1))
        }

        mv.mark(end)
        return result
    }


    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        val asmType = expression.typeOperand.toKotlinType().asmType
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                val result = expression.argument.accept(this, data)
                expression.argument.markEndOfStatementIfNeeded()
                coerce(result.type, Type.VOID_TYPE, mv)
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
                generateAsCast(
                    mv, expression.typeOperand.toKotlinType(), boxedType, expression.operator == IrTypeOperator.SAFE_CAST,
                    state.languageVersionSettings.isReleaseCoroutines()
                )
                return onStack(boxedType)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> {
                gen(expression.argument, OBJECT_TYPE, data)
                val type = boxType(asmType)
                generateIsCheck(mv, expression.typeOperand.toKotlinType(), type, state.languageVersionSettings.isReleaseCoroutines())
                if (IrTypeOperator.NOT_INSTANCEOF == expression.operator) {
                    StackValue.not(StackValue.onStack(Type.BOOLEAN_TYPE)).put(Type.BOOLEAN_TYPE, mv)
                }
            }

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val value = gen(expression.argument, data)
                mv.dup()
                mv.visitLdcInsn("TODO provide message for IMPLICIT_NOTNULL") /*TODO*/
                mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                    "(Ljava/lang/Object;Ljava/lang/String;)V", false
                )
                StackValue.onStack(value.type).put(asmType, mv)
            }

            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> {
                gen(expression.argument, Type.INT_TYPE, data)
                StackValue.coerce(Type.INT_TYPE, typeMapper.mapType(expression.type.toKotlinType()), mv)
            }
        }
        return expression.onStack
    }

    private fun IrExpression.markEndOfStatementIfNeeded() {
        when (this) {
            is IrWhen -> if (this.branches.size > 1) {
                this.markLineNumber(false)
            }
            is IrTry -> this.markLineNumber(false)
            is IrContainerExpression -> when (this.origin) {
                IrStatementOrigin.WHEN, IrStatementOrigin.IF ->
                    this.markLineNumber(false)
            }
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        AsmUtil.genStringBuilderConstructor(mv)
        expression.arguments.forEach {
            val stackValue = gen(it, data)
            AsmUtil.genInvokeAppendMethod(mv, stackValue.type, stackValue.kotlinType)
        }

        mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        return expression.onStack
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: BlockInfo): StackValue {
        val continueLabel = markNewLabel()
        val endLabel = Label()
        val condition = loop.condition

        // Avoid true condition generation for tailrec
        // ASM and Java verifier assumes that L1 is reachable that cause several verification to fail,
        // to avoid them trivial jump elumination is required
        // L0
        // ICONST_1 //could be eliminated
        // IFEQ L1 //could be eliminated
        // .... // no jumps
        // GOTO L0
        // L1
        //TODO: write elimination lower
        condition.markLineNumber(startOffset = true)
        if (!(condition is IrConst<*> && condition.value == true)) {
            gen(condition, data)
            BranchedValue.condJump(StackValue.onStack(condition.asmType), endLabel, true, mv)
        } else {
            mv.fakeAlwaysFalseIfeq(endLabel)
        }

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
        jump.markLineNumber(startOffset = true)
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

        mv.fakeAlwaysFalseIfeq(continueLabel)
        mv.fakeAlwaysFalseIfeq(endLabel)

        with(LoopInfo(loop, continueLabel, endLabel)) {
            data.addInfo(this)
            loop.body?.apply {
                gen(this, data)
            }
            data.removeInfo(this)
        }

        mv.visitLabel(continueLabel)
        val condition = loop.condition
        condition.markLineNumber(startOffset = true)
        gen(condition, data)
        BranchedValue.condJump(StackValue.onStack(condition.asmType), entry, false, mv)
        mv.mark(endLabel)

        return loop.onStack
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): StackValue {
        aTry.markLineNumber(startOffset = true)
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
        genFinallyBlockOrGoto(tryInfo, tryCatchBlockEnd, null, data)

        val catches = aTry.catches
        for (clause in catches) {
            val clauseStart = markNewLabel()
            val descriptor = clause.parameter
            val descriptorType = descriptor.asmType
            val index = frame.enter(clause.catchParameter, descriptorType)
            mv.store(index, descriptorType)

            val catchBody = clause.result
            catchBody.markLineNumber(true)
            gen(catchBody, catchBody.asmType, data)

            frame.leave(clause.catchParameter)

            val clauseEnd = markNewLabel()

            mv.visitLocalVariable(
                descriptor.name.asString(), descriptorType.descriptor, null, clauseStart, clauseEnd,
                index
            )

            genFinallyBlockOrGoto(
                tryInfo,
                if (clause != catches.last() || finallyExpression != null) tryCatchBlockEnd else null,
                null,
                data
            )

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
            if (tryInfo != null) {
                tryInfo.tryBlock.finallyExpression!!.markLineNumber(startOffset = false)
            }
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
            } else {
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
        expression.markLineNumber(startOffset = true)
        gen(expression.value, JAVA_THROWABLE_TYPE, data)
        mv.athrow()
        return none()
    }

    override fun visitGetClass(expression: IrGetClass, data: BlockInfo): StackValue {
        generateClassLiteralReference(expression, true, data)
        return expression.onStack
    }

    override fun visitClassReference(expression: IrClassReference, data: BlockInfo): StackValue {
        generateClassLiteralReference(expression, true, data)
        return expression.onStack
    }

    fun generateClassLiteralReference(
        classReference: IrExpression,
        wrapIntoKClass: Boolean,
        data: BlockInfo
    ) {
        if (classReference !is IrClassReference /* && DescriptorUtils.isObjectQualifier(classReference.descriptor)*/) {
            assert(classReference is IrGetClass)
            JavaClassProperty.generateImpl(mv, gen((classReference as IrGetClass).argument, data))
        } else {
            val classType = classReference.classType
            if (classType is CrIrType) {
                putJavaLangClassInstance(mv, classType.type)
                return
            } else {
                val type = classType.toKotlinType()
                if (TypeUtils.isTypeParameter(type)) {
                    assert(TypeUtils.isReifiedTypeParameter(type)) { "Non-reified type parameter under ::class should be rejected by type checker: " + type }
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.JAVA_CLASS, mv, this)
                }

                putJavaLangClassInstance(mv, typeMapper.mapType(type))
            }
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }

    }

    private fun coerceNotToUnit(fromType: Type, fromKotlinType: KotlinType?, toKotlinType: KotlinType): StackValue {
        val asmToType = toKotlinType.asmType
        if (asmToType != AsmTypes.UNIT_TYPE || TypeUtils.isNullableType(toKotlinType)) {
            coerce(fromType, fromKotlinType, asmToType, toKotlinType, mv)
            return onStack(asmToType, toKotlinType)
        }
        return onStack(fromType, fromKotlinType)
    }

    val IrExpression.asmType: Type
        get() = typeMapper.mapType(this.type.toKotlinType())

    val IrExpression.onStack: StackValue
        get() = StackValue.onStack(this.asmType)

    private fun resolveToCallable(irCall: IrMemberAccessExpression, isSuper: Boolean): Callable {
        val intrinsic = intrinsics.getIntrinsic(irCall.descriptor.original as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(
                irCall,
                typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor),
                classCodegen.context
            )
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
            descriptor = if (descriptor is PropertyGetterDescriptor) {
                propertyDescriptor.getMethod
            } else {
                propertyDescriptor.setMethod!!
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
        val isInline = descriptor.isInlineCall(state)

        if (!isInline) return IrCallGenerator.DefaultCallGenerator

        val original = unwrapInitialSignatureDescriptor(DescriptorUtils.unwrapFakeOverride(descriptor.original as FunctionDescriptor))
        return if (isDefaultCompilation) {
            TODO()
        } else {
            IrInlineCodegen(this, state, original, typeParameterMappings!!, IrSourceCompilerForInline(state, element, this))
        }
    }

    internal fun getOrCreateCallGenerator(
        memberAccessExpression: IrMemberAccessExpression,
        descriptor: CallableDescriptor
    ): IrCallGenerator {
        val typeArguments =
            if (memberAccessExpression.typeArgumentsCount == 0) {
                //avoid ambiguity with type constructor type parameters
                emptyMap()
            } else descriptor.original.typeParameters.keysToMap {
                memberAccessExpression.getTypeArgumentOrDefault(it)
            }

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
            } else {
                mappings.addParameterMappingForFurtherReification(
                    key.name.identifier, type, typeParameterAndReificationArgument.second, isReified
                )
            }
        }

        return getOrCreateCallGenerator(descriptor, memberAccessExpression, mappings, false)
    }

    override val frameMap: IrFrameMap
        get() = frame
    override val visitor: InstructionAdapter
        get() = mv
    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo")

    override var lastLineNumber: Int = -1

    override fun consumeReifiedOperationMarker(typeParameterDescriptor: TypeParameterDescriptor) {
        //TODO
    }

    override fun propagateChildReifiedTypeParametersUsages(reifiedTypeParametersUsages: ReifiedTypeParametersUsages) {
        //TODO
    }

    override fun pushClosureOnStack(
        classDescriptor: ClassDescriptor,
        putThis: Boolean,
        callGenerator: CallGenerator,
        functionReferenceReceiver: StackValue?
    ) {
        //TODO
    }

    override fun markLineNumberAfterInlineIfNeeded() {
        //TODO
    }

    private fun markNewLabel() = Label().apply { mv.visitLabel(this) }

    private fun IrElement.markLineNumber(startOffset: Boolean) {
        val offset = if (startOffset) this.startOffset else endOffset
        if (offset < 0) {
            return
        }
        val lineNumber = fileEntry.getLineNumber(offset) + 1
        assert(lineNumber > 0)
        if (lastLineNumber == lineNumber) {
            return
        }
        lastLineNumber = lineNumber
        mv.visitLineNumber(lineNumber, markNewLabel())
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
        callGenerator.putValueIfNeeded(
            parameterType,
            StackValue.constant(null, parameterType),
            ValueKind.METHOD_HANDLE_IN_DEFAULT,
            -1,
            codegen
        )
    }
    return toInts.isNotEmpty()
}

internal fun CallableDescriptor.isInlineCall(state: GenerationState) =
    (!state.isInlineDisabled || InlineUtil.containsReifiedTypeParameters(this)) &&
            (InlineUtil.isInline(this) || InlineUtil.isArrayConstructorWithLambda(this))
