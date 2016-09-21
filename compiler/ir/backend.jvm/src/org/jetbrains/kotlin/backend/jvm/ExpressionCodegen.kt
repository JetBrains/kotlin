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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicFunction
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.codegen.BranchedValue
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.StackValue
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
        val classCodegen: JvmClassCodegen
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
            coerceNotToUnit(r.type, Type.VOID_TYPE)
            exp.accept(this, data)
        }
        coerceNotToUnit(result.type, expression.asmType)
        return expression.onStack
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: BlockInfo): StackValue {
        return generateCall(expression, null, data)
    }

    override fun visitCall(expression: IrCall, data: BlockInfo): StackValue {
        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrMemberAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): StackValue {
        val callable = resolveToCallable(expression, superQualifier != null)
        if (callable is IrIntrinsicFunction) {
            callable.invoke(mv, this, data)
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
            if (expression !is ConstructorDescriptor) {
                coerce(callable.returnType, expression.asmType, mv)
            }
        }
        if (expression.descriptor is ConstructorDescriptor) {
            return none()
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
            gen(this, varType, data)
            StackValue.local(index, varType).store(StackValue.onStack(this.asmType), mv)
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

    fun gen(expression: IrExpression, type: Type, data: BlockInfo) {
        expression.accept(this, data)
        StackValue.onStack(expression.asmType).put(type, mv)
    }

    fun gen(expression: IrExpression, data: BlockInfo) {
        gen(expression, expression.asmType, data)
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
        expression.value.accept(this, data)
        StackValue.local(frame.getIndex(expression.descriptor), expression.asmType).store(StackValue.onStack(expression.value.asmType), mv)
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

    fun markNewLabel(): Label {
        return Label().apply { mv.visitLabel(this) }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): StackValue {
        expression.value?.accept(this, data)
        mv.areturn(expression.asmType)
        return expression.onStack
    }


    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
        /*TODO */
        if (expression is IrIfThenElseImpl) {
            val elseLabel = Label()
            val condition = expression.getNthCondition(0)!!
            gen(condition, data)
            BranchedValue.condJump(StackValue.onStack(condition.asmType), elseLabel, true, mv)

            val end = Label()

            expression.getNthResult(0)!!.apply {
                gen(this, data)
                coerceNotToUnit(this.asmType, expression.asmType)
            }

            mv.goTo(end)
            mv.mark(elseLabel)

            expression.getNthResult(1)?.apply {
                gen(this, data)
                coerceNotToUnit(this.asmType, expression.asmType)
            }

            mv.mark(end)
        } else {
            super.visitWhen(expression, data)
        }
        return expression.onStack
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            expression.argument.accept(this, data)
            coerce(expression.argument.asmType, Type.VOID_TYPE, mv)
            return none()
        } else {
            return super.visitTypeOperator(expression, data)
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
        val intrinsic = intrinsics.getIntrinsic(irCall.descriptor as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(irCall, typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor), classCodegen.context)
        }

        return typeMapper.mapToCallableMethod(irCall.descriptor as FunctionDescriptor, isSuper)
    }
}

