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
import org.jetbrains.kotlin.codegen.AsmUtil.boxType
import org.jetbrains.kotlin.codegen.AsmUtil.correctElementType
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class BlockInfo {
    val variables: MutableList<VariableInfo> = mutableListOf()
}

class VariableInfo(val name: String, val index: Int, val type: Type, val startLabel: Label)

class ExpressionCodegen(
        val irFunction: IrFunction,
        val frame: FrameMap,
        val mv: InstructionAdapter,
        val classCodegen: ClassCodegen
) : IrElementVisitor<StackValue, BlockInfo> {

    /*TODO*/
    val intrinsics = IrIntrinsicMethods(classCodegen.context.irBuiltIns)

    val typeMapper = classCodegen.typeMapper

    fun generate() {
        mv.visitCode()
        val info = BlockInfo()
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
            r, exp ->
            exp.accept(this, data)
        }
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): StackValue {
        val info = BlockInfo()
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
            mv.visitLocalVariable(it.name, it.type.descriptor, null, it.startLabel, endLabel, it.index)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo): StackValue {
        val result = expression.statements.fold(none()) {
            r, exp ->
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
            val receiver = expression.dispatchReceiver
            receiver?.apply {
                gen(receiver, callable.owner, data)
            }
            val args = (listOf(expression.extensionReceiver) +
                       expression.descriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
                           expression.getValueArgument(i)
                       }).filterNotNull()
            args.forEachIndexed { i, expression ->
                gen(expression, callable.parameterTypes[i], data)
            }

            callable.genInvokeInstruction(mv)
            return StackValue.onStack(callable.returnType)
//            if (expression.descriptor !is ConstructorDescriptor) {
//                //coerce(callable.returnType, expression.asmType, mv)
//            }
        }

        return expression.onStack
    }



    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BlockInfo): StackValue {
        return none()
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
                declaration.descriptor.name.asString(),
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

    override fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver, data: BlockInfo): StackValue {
        return generateLocal(expression.descriptor, expression.asmType)
    }

    override fun visitGetVariable(expression: IrGetVariable, data: BlockInfo): StackValue {
        return generateLocal(expression.descriptor, expression.asmType)
    }

    private fun generateLocal(descriptor: CallableDescriptor, type: Type): StackValue {
        StackValue.local(frame.getIndex(descriptor), type).put(type, mv)
        return onStack(type)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): StackValue {
        val value = expression.value.accept(this, data)
        StackValue.local(frame.getIndex(expression.descriptor), expression.descriptor.asmType).store(value, mv)
        //UNIT?
        return expression.onStack
    }

    override fun visitThisReference(expression: IrThisReference, data: BlockInfo): StackValue {
        StackValue.local(0, expression.asmType).put(expression.asmType, mv)
        return expression.onStack
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

    override fun visitVararg(expression: IrVararg, data: BlockInfo): StackValue {
        val size = expression.elements.size
        mv.iconst(size)
        val varargType = expression.asmType
        val elementType = correctElementType(varargType)
        val asmType = elementType
        newArrayInstruction(expression.type)
        for ((i, element)  in expression.elements.withIndex()) {
            mv.dup()
            StackValue.constant(i, Type.INT_TYPE).put(Type.INT_TYPE, mv)
            val rightSide = gen(element, asmType, data)
            StackValue.arrayElement(asmType, StackValue.onStack(asmType), StackValue.onStack(Type.INT_TYPE)).store(rightSide, mv)
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
        val value = expression.value?.apply {
            gen(this, this.asmType, data)
        }
        mv.areturn((expression.value ?: expression).asmType)
        return expression.onStack
    }


    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
        val resultType = expression.asmType
        genIfWithBranches(expression.branches[0].condition, expression.branches[0].result, data, resultType, expression.branches.drop(1))
        return expression.onStack
    }


    fun genIfWithBranches(condition: IrExpression, thenBranch: IrExpression, data: BlockInfo, type: Type, otherBranches: List<IrBranch>) {
        val elseLabel = Label()

        //TODO don't generate condition for else branch - java verifier fails with empty stack
        val elseBranch = condition is IrConst<*> && true == condition.value
        if (!elseBranch) {
            gen(condition, data)
            BranchedValue.condJump(StackValue.onStack(condition.asmType), elseLabel, true, mv)
        }

        val end = Label()

        thenBranch.apply {
            gen(this, data)
            coerceNotToUnit(this.asmType, type)
        }

        mv.goTo(end)
        mv.mark(elseLabel)

        if (!otherBranches.isEmpty()) {
            val nextBranch = otherBranches.first()
            genIfWithBranches(nextBranch.condition, nextBranch.result, data, type, otherBranches.drop(1))
        }

        mv.mark(end)
    }


    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.accept(this, data)
                coerce(expression.argument.asmType, Type.VOID_TYPE, mv)
                return none()
            }

            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> {
                val value = expression.argument.accept(this, data)
                value.put(boxType(value.type), mv)
                if (value.type === Type.VOID_TYPE) {
                    StackValue.putUnitInstance(mv)
                }
                generateAsCast(mv, expression.typeOperand, boxType(expression.typeOperand.asmType), expression.operator == IrTypeOperator.SAFE_CAST)
            }

            else -> super.visitTypeOperator(expression, data)
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
        val entry = Label()
        mv.visitLabel(entry)

        val condition = loop.condition
        gen(condition, data)
        val endLabel = Label()
        BranchedValue.condJump(StackValue.onStack(condition.asmType), endLabel, true, mv)

        loop.body?.apply {
            gen(this, data)
        }
        mv.goTo(entry)
        mv.mark(endLabel)

        return loop.onStack
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BlockInfo): StackValue {
        return super.visitDoWhileLoop(loop, data)
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
        val intrinsic = intrinsics.getIntrinsic(irCall.descriptor as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(irCall, typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor), classCodegen.context)
        }

        return typeMapper.mapToCallableMethod(irCall.descriptor as FunctionDescriptor, isSuper)
    }

    private val KotlinType.asmType: Type
        get() = typeMapper.mapType(this)

    private val CallableDescriptor.asmType: Type
        get() = typeMapper.mapType(this)
}

