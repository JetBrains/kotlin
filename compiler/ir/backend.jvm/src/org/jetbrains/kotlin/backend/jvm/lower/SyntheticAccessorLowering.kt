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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.codegen.AccessorForCallableDescriptor
import org.jetbrains.kotlin.codegen.AccessorForPropertyDescriptor
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import kotlin.properties.Delegates

interface StubContext {
    val irClassContext: IrClassContext
}

class StubCodegenContext(
        contextDescriptor: ClassDescriptor,
        parentContext: CodegenContext<*>?,
        override val irClassContext: IrClassContext
) :StubContext, CodegenContext<DeclarationDescriptor>(
        if (contextDescriptor is FileClassDescriptor) contextDescriptor.containingDeclaration else contextDescriptor,
        OwnerKind.IMPLEMENTATION, parentContext, null,
        if (contextDescriptor is FileClassDescriptor) null else contextDescriptor,
        null
)

class ClassStubContext(
        contextDescriptor: ClassDescriptor,
        parentContext: CodegenContext<*>?,
        override val irClassContext: IrClassContext,
        typeMapper: KotlinTypeMapper
) : StubContext, ClassContext( typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null)

class ContextAnnotator(val state: GenerationState) : ClassLowerWithContext() {

    val context2Codegen = hashMapOf<IrClassContext, CodegenContext<*>>()
    val class2Codegen = hashMapOf<ClassDescriptor, CodegenContext<*>>()

    private val IrClassContext.codegenContext: CodegenContext<*>
        get() = context2Codegen[this]!!

    private val ClassDescriptor.codegenContext: CodegenContext<*>
        get() = class2Codegen[this]!!


    override fun lowerBefore(irClass: IrClass, data: IrClassContext) {
        val descriptor = irClass.descriptor
        val newContext: CodegenContext<*> = if (descriptor is FileClassDescriptor || descriptor !is ClassDescriptor) {
            StubCodegenContext(descriptor, data.parent?.codegenContext, data)
        }
        else {
            ClassStubContext(descriptor, data.parent?.codegenContext, data, state.typeMapper)
        }
        newContext.apply {
            context2Codegen.put(data, this)
            class2Codegen.put(descriptor, this)
        }
    }

    override fun lower(irCLass: IrClass, data: IrClassContext) {

    }
}

class SyntheticAccessorLowering(val state: GenerationState) : FileLoweringPass, IrElementTransformer<IrClassContext?> {

    private val IrClassContext.codegenContext: CodegenContext<*>
        get() = contextAnnotator.context2Codegen[this]!!

    var contextAnnotator by Delegates.notNull<ContextAnnotator>()

    private val ClassDescriptor.codegenContext: CodegenContext<*>
        get() = contextAnnotator.class2Codegen[this]!!

    override fun lower(irFile: IrFile) {
        contextAnnotator = ContextAnnotator(state)
        contextAnnotator.lower(irFile)
        irFile.transform(this, null)
    }

    override fun visitClass(declaration: IrClass, data: IrClassContext?): IrStatement {
        val classContext = (declaration.descriptor.codegenContext as StubContext).irClassContext
        return super.visitClass(declaration, classContext).apply {
            lower(this as IrClass, classContext)
        }
    }

    fun lower(irCLass: IrClass, data: IrClassContext) {
        val codegenContext = data.codegenContext
        val accessors = codegenContext.accessors
        val allAccessors =
                (
                        accessors.filterIsInstance<FunctionDescriptor>() +
                        accessors.filterIsInstance<AccessorForPropertyDescriptor>().flatMap {
                            listOf(if (it.isWithSyntheticGetterAccessor) it.getter else null, if (it.isWithSyntheticSetterAccessor) it.setter else null).filterNotNull()
                        }
                ).filterIsInstance<AccessorForCallableDescriptor<*>>()
        allAccessors.forEach { accessor ->
            val accessorOwner = (accessor as FunctionDescriptor).containingDeclaration as ClassOrPackageFragmentDescriptor
            val body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val accessorDescriptor = accessor.toStatic(accessorOwner, Name.identifier(state.typeMapper.mapAsmMethod(accessor).name))
            val syntheticFunction = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                    accessorDescriptor, body
            )
            val calleeDescriptor = accessor.calleeDescriptor as FunctionDescriptor
            val returnExpr = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, calleeDescriptor)
            copyAllArgsToValueParams(returnExpr, accessorDescriptor)
            body.statements.add(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, accessor, returnExpr))
            data.irClass.declarations.add(syntheticFunction)
        }
    }


    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: IrClassContext?): IrElement {
        val superResult = super.visitMemberAccess(expression, data)
        val descriptor = expression.descriptor
        if (descriptor is FunctionDescriptor) {
            val directAccessor = data!!.codegenContext.accessibleDescriptor(JvmCodegenUtil.getDirectMember(descriptor), (expression as? IrCall)?.superQualifier)
            val accessor = actualAccessor(descriptor, directAccessor)

            if (accessor is AccessorForCallableDescriptor<*> && descriptor !is AccessorForCallableDescriptor<*>) {
                val accessorOwner = accessor.containingDeclaration as ClassOrPackageFragmentDescriptor
                val staticAccessor = descriptor.toStatic(accessorOwner, Name.identifier(state.typeMapper.mapAsmMethod(accessor as FunctionDescriptor).name)) //TODO change call
                val call = IrCallImpl(expression.startOffset, expression.endOffset, staticAccessor, emptyMap(), expression.origin/*TODO super*/)
                //copyAllArgsToValueParams(call, expression)
                expression.receiverAndArgs().forEachIndexed { i, irExpression ->
                    call.putValueArgument(i, irExpression)
                }
                return call
            }
        }
        return superResult
    }

    private fun actualAccessor(descriptor: FunctionDescriptor, calculatedAccessor: CallableMemberDescriptor): CallableMemberDescriptor {
        if (calculatedAccessor is AccessorForPropertyDescriptor) {
            val isGetter = descriptor is PropertyGetterDescriptor
            val propertyAccessor = if (isGetter) calculatedAccessor.getter!! else calculatedAccessor.setter!!
            if (isGetter && calculatedAccessor.isWithSyntheticGetterAccessor || !isGetter && calculatedAccessor.isWithSyntheticSetterAccessor) {
                return propertyAccessor
            }
            return descriptor

        }
        return calculatedAccessor
    }

    private fun copyAllArgsToValueParams(call: IrCallImpl, fromDescriptor: CallableMemberDescriptor) {
        var offset = 0
        val newDescriptor = call.descriptor
        newDescriptor.dispatchReceiverParameter?.let {
            call.dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, fromDescriptor.valueParameters[offset++])
        }

        newDescriptor.extensionReceiverParameter?.let {
            call.extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, fromDescriptor.valueParameters[offset++])
        }

        call.descriptor.valueParameters.forEachIndexed { i, _ ->
            call.putValueArgument(i, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, fromDescriptor.valueParameters[i + offset]))
        }
    }
}