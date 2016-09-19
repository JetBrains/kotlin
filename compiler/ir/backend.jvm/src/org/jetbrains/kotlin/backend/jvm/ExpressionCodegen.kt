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
import org.jetbrains.kotlin.codegen.BranchedValue
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.StackValue.expression
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

//Combine to StackValue?
class Output
class BlockInfo {
    val variables: MutableList<VariableInfo> = mutableListOf()
}
/*TODO*/
val intrinsics = IntrinsicMethods()

class VariableInfo(val name: String, val index: Int, val type: Type, val startLabel: Label)

class ExpressionCodegen(
        val irFunction: IrFunction,
        val frame: FrameMap,
        val mv: InstructionAdapter,
        val classCodegen: JvmClassCodegen
) : IrElementVisitor<Output?, BlockInfo> {

    val typeMapper = classCodegen.typeMapper

    fun generate() {
        mv.visitCode()
        val info = BlockInfo()
        irFunction.body?.accept(this, info)
        val returnType = typeMapper.mapReturnType(irFunction.descriptor)
        if (returnType == Type.VOID_TYPE) {
            //for implicit return
            mv.areturn(Type.VOID_TYPE)
        }
        writeLocalVariablesInTable(info)
        mv.visitEnd()
    }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): Output? {
        body.statements.forEach { it.accept(this, data) }
        return null
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): Output? {
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

    override fun visitCall(expression: IrCall, data: BlockInfo): Output? {
        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrCall, superQualifier: ClassDescriptor?, data: BlockInfo): Output? {
        val callable = resolveToCallable(expression, superQualifier != null)
        if (callable is IrIntrinsicFunction) {
            callable.invoke(mv, this, data)
        } else {
            expression.dispatchReceiver?.accept(this, data)
            expression.extensionReceiver?.accept(this, data)
            expression.descriptor.valueParameters.forEachIndexed { i, valueParameterDescriptor ->
                expression.getValueArgument(i)?.accept(this, data)
                //coerce?
            }

            callable.genInvokeInstruction(mv)
        }
        return null
    }



    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BlockInfo): Output? {
        return null
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BlockInfo): Output? {
        //HACK
        StackValue.local(0, OBJECT_TYPE).put(OBJECT_TYPE, mv)
        return super.visitDelegatingConstructorCall(expression, data)
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): Output? {
        val varType = typeMapper.mapType(declaration.descriptor)
        val index = frame.enter(declaration.descriptor, varType)

        declaration.initializer?.apply {
            accept(this@ExpressionCodegen, data)
            StackValue.local(index, varType).store(StackValue.onStack(this.asmType), mv)
        }

        val info = VariableInfo(
                declaration.descriptor.name.asString(),
                index,
                varType,
                markNewLabel()
        )
        data.variables.add(info)

        return null
    }

    fun gen(expression: IrExpression, type: Type, data: BlockInfo) {
        expression.accept(this, data)
        StackValue.onStack(expression.asmType).put(type, mv)
    }

    fun gen(expression: IrExpression, data: BlockInfo) {
        gen(expression, expression.asmType, data)
    }

    override fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver, data: BlockInfo): Output? {
        generateLocal(expression.descriptor, expression.asmType)
        return null
    }

    override fun visitGetVariable(expression: IrGetVariable, data: BlockInfo): Output? {
        generateLocal(expression.descriptor, expression.asmType)
        return null
    }

    private fun generateLocal(descriptor: CallableDescriptor, type: Type) {
        StackValue.local(frame.getIndex(descriptor), type).put(type, mv)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): Output? {
        expression.value.accept(this, data)
        StackValue.local(frame.getIndex(expression.descriptor), expression.asmType).store(StackValue.onStack(expression.value.asmType), mv)
        return null
    }

    override fun visitThisReference(expression: IrThisReference, data: BlockInfo): Output? {
        StackValue.local(0, expression.asmType).put(expression.asmType, mv)
        return null
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): Output? {
        val value = expression.value
        val type = expression.asmType
        StackValue.constant(value, type).put(type, mv)
        return null
    }

    override fun visitElement(element: IrElement, data: BlockInfo): Output? {
        TODO("not implemented for $element") //To change body of created functions use File | Settings | File Templates.
    }

    fun markNewLabel(): Label {
        return Label().apply { mv.visitLabel(this) }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): Output? {
        expression.value?.accept(this, data)
        mv.areturn(expression.asmType)
        return null
    }


    override fun visitWhen(expression: IrWhen, data: BlockInfo): Output? {
        if (IrStatementOrigin.IF == expression.origin) {
            val elseLabel = Label()
            val condition = expression.getNthCondition(0)!!
            gen(condition, data)
            BranchedValue.condJump(StackValue.onStack(condition.asmType), elseLabel, true, mv)

            val end = Label()

            gen(expression.getNthResult(0)!!, data)

            mv.goTo(end)
            mv.mark(elseLabel)

            expression.getNthResult(1)?.apply {
                gen(this, data)
            }

            mv.mark(end)
        } else {
            super.visitWhen(expression, data)
        }
        return null
    }

    val IrExpression.asmType: Type
        get() = typeMapper.mapType(this.type)

    private fun resolveToCallable(irCall: IrCall, isSuper: Boolean): Callable {
        val intrinsic = intrinsics.getIntrinsic(irCall.descriptor as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(irCall, typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor), classCodegen.context)
        }

        return typeMapper.mapToCallableMethod(irCall.descriptor as FunctionDescriptor, isSuper)
    }
}

