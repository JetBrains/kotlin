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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.AbstractClosureAnnotator
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.Closure
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class LocalFunctionsLowering(val context: BackendContext): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                LocalFunctionsTransformer(memberDeclaration).lowerLocalFunctions()
            else
                null
        }
    }

    private class LocalFunctionContext(val declaration: IrFunction) {
        lateinit var closure: Closure

        val closureParametersCount: Int get() = closure.capturedValues.size

        lateinit var transformedDescriptor: FunctionDescriptor

        val old2new: MutableMap<ValueDescriptor, ParameterDescriptor> = HashMap()

        var index: Int = -1

        override fun toString(): String =
                "LocalFunctionContext for ${declaration.descriptor}"
    }

    private inner class LocalFunctionsTransformer(val memberFunction: IrFunction) {
        val localFunctions: MutableMap<FunctionDescriptor, LocalFunctionContext> = LinkedHashMap()
        val new2old: MutableMap<ParameterDescriptor, ValueDescriptor> = HashMap()

        fun lowerLocalFunctions(): List<IrDeclaration>? {
            collectLocalFunctions()
            if (localFunctions.isEmpty()) return null

            collectClosures()

            transformDescriptors()

            rewriteBodies()

            return collectRewrittenDeclarations()
        }

        private fun collectRewrittenDeclarations(): ArrayList<IrDeclaration> =
                ArrayList<IrDeclaration>(localFunctions.size + 1).apply {
                    add(memberFunction)

                    localFunctions.values.mapTo(this) {
                        val original = it.declaration
                        IrFunctionImpl(
                                original.startOffset, original.endOffset, original.origin,
                                it.transformedDescriptor,
                                original.body
                        )
                    }
                }

        private inner class FunctionBodiesRewriter(val localFunctionContext: LocalFunctionContext?) : IrElementTransformerVoid() {

            override fun visitClass(declaration: IrClass): IrStatement {
                // ignore local classes for now
                return declaration
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                // replace local function definition with an empty composite
                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.builtIns.unitType)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val remapped = localFunctionContext?.let { it.old2new[expression.descriptor] }
                return if (remapped == null)
                    expression
                else
                    IrGetValueImpl(expression.startOffset, expression.endOffset, remapped, expression.origin)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.descriptor.original
                val localFunctionData = localFunctions[oldCallee] ?: return expression

                val newCallee = localFunctionData.transformedDescriptor

                val newCall = createNewCall(expression, newCallee).fillArguments(localFunctionData, expression)

                return newCall
            }

            private fun <T : IrMemberAccessExpression> T.fillArguments(calleeContext: LocalFunctionContext, oldExpression: IrMemberAccessExpression): T {
                val closureParametersCount = calleeContext.closureParametersCount

                mapValueParametersIndexed { index, newValueParameterDescriptor ->
                    val capturedValueDescriptor = new2old[newValueParameterDescriptor] ?:
                                                  throw AssertionError("Non-mapped parameter $newValueParameterDescriptor")
                    if (index >= closureParametersCount)
                        oldExpression.getValueArgument(capturedValueDescriptor as ValueParameterDescriptor)
                    else {
                        val remappedValueDescriptor = localFunctionContext?.let { it.old2new[capturedValueDescriptor] }
                        IrGetValueImpl(oldExpression.startOffset, oldExpression.endOffset,
                                       remappedValueDescriptor ?: capturedValueDescriptor)
                    }

                }

                dispatchReceiver = oldExpression.dispatchReceiver
                extensionReceiver = oldExpression.extensionReceiver

                return this
            }

            override fun visitCallableReference(expression: IrCallableReference): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.descriptor.original
                val localFunctionData = localFunctions[oldCallee] ?: return expression
                val newCallee = localFunctionData.transformedDescriptor

                val newCallableReference = IrCallableReferenceImpl(
                        expression.startOffset, expression.endOffset,
                        expression.type, // TODO functional type for transformed descriptor
                        newCallee,
                        remapTypeArguments(expression, newCallee),
                        expression.origin
                ).fillArguments(localFunctionData, expression)

                return newCallableReference
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid(this)

                val oldReturnTarget = expression.returnTarget
                val localFunctionData = localFunctions[oldReturnTarget] ?: return expression
                val newReturnTarget = localFunctionData.transformedDescriptor

                return IrReturnImpl(expression.startOffset, expression.endOffset, newReturnTarget, expression.value)
            }
        }

        private fun rewriteFunctionDeclaration(irFunction: IrFunction, localFunctionContext: LocalFunctionContext?) {
            irFunction.transformChildrenVoid(FunctionBodiesRewriter(localFunctionContext))
        }

        private fun rewriteBodies() {
            localFunctions.values.forEach {
                rewriteFunctionDeclaration(it.declaration, it)
            }

            rewriteFunctionDeclaration(memberFunction, null)
        }

        private fun createNewCall(oldCall: IrCall, newCallee: FunctionDescriptor) =
                if (oldCall is IrCallWithShallowCopy)
                    oldCall.shallowCopy(oldCall.origin, newCallee, oldCall.superQualifier)
                else
                    IrCallImpl(
                            oldCall.startOffset, oldCall.endOffset,
                            newCallee,
                            remapTypeArguments(oldCall, newCallee),
                            oldCall.origin, oldCall.superQualifier
                    )

        private fun remapTypeArguments(oldExpression: IrMemberAccessExpression, newCallee: FunctionDescriptor): Map<TypeParameterDescriptor, KotlinType>? {
            val oldCallee = oldExpression.descriptor

            return if (oldCallee.typeParameters.isEmpty())
                null
            else oldCallee.typeParameters.associateBy(
                    { newCallee.typeParameters[it.index] },
                    { oldExpression.getTypeArgument(it)!! }
            )
        }

        private fun transformDescriptors() {
            localFunctions.values.forEach {
                it.transformedDescriptor = createTransformedDescriptor(it)
            }
        }

        private fun suggestLocalName(descriptor: DeclarationDescriptor): String {
            val localFunctionContext = localFunctions[descriptor]
            return if (localFunctionContext != null && localFunctionContext.index >= 0)
                "lambda-${localFunctionContext.index}"
            else
                descriptor.name.asString()
        }

        private fun generateNameForLiftedFunction(functionDescriptor: FunctionDescriptor): Name =
                Name.identifier(
                        functionDescriptor.parentsWithSelf
                                .takeWhile { it is FunctionDescriptor }
                                .toList().reversed()
                                .map { suggestLocalName(it) }
                                .joinToString(separator = "$")
                        )

        private fun createTransformedDescriptor(localFunctionContext: LocalFunctionContext): FunctionDescriptor {
            val oldDescriptor = localFunctionContext.declaration.descriptor

            val memberOwner = memberFunction.descriptor.containingDeclaration
            val newDescriptor = SimpleFunctionDescriptorImpl.create(
                    memberOwner,
                    oldDescriptor.annotations,
                    generateNameForLiftedFunction(oldDescriptor),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    oldDescriptor.source
            )

            val closureParametersCount = localFunctionContext.closureParametersCount
            val newValueParametersCount = closureParametersCount + oldDescriptor.valueParameters.size

            val newDispatchReceiverParameter =
                    if (memberOwner is ClassDescriptor && oldDescriptor.dispatchReceiverParameter != null)
                        memberOwner.thisAsReceiverParameter
                    else
                        null

            // Do not substitute type parameters for now.
            val newTypeParameters = oldDescriptor.typeParameters

            val newValueParameters = ArrayList<ValueParameterDescriptor>(newValueParametersCount).apply {
                localFunctionContext.closure.capturedValues.mapIndexedTo(this) { i, capturedValueDescriptor ->
                    createUnsubstitutedCapturedValueParameter(newDescriptor, capturedValueDescriptor, i).apply {
                        localFunctionContext.recordRemapped(capturedValueDescriptor, this)
                    }
                }

                oldDescriptor.valueParameters.mapIndexedTo(this) { i, oldValueParameterDescriptor ->
                    createUnsubstitutedParameter(newDescriptor, oldValueParameterDescriptor, closureParametersCount + i).apply {
                        localFunctionContext.recordRemapped(oldValueParameterDescriptor, this)
                    }
                }
            }

            newDescriptor.initialize(
                    oldDescriptor.extensionReceiverParameter?.type,
                    newDispatchReceiverParameter,
                    newTypeParameters,
                    newValueParameters,
                    oldDescriptor.returnType,
                    Modality.FINAL,
                    Visibilities.PRIVATE
            )

            oldDescriptor.extensionReceiverParameter?.let {
                localFunctionContext.recordRemapped(it, newDescriptor.extensionReceiverParameter!!)
            }

            return newDescriptor
        }

        private fun LocalFunctionContext.recordRemapped(oldDescriptor: ValueDescriptor, newDescriptor: ParameterDescriptor): ParameterDescriptor {
            old2new[oldDescriptor] = newDescriptor
            new2old[newDescriptor] = oldDescriptor
            return newDescriptor
        }

        private fun suggestNameForCapturedValueParameter(valueDescriptor: ValueDescriptor): Name =
                if (valueDescriptor.name.isSpecial) {
                    val oldNameStr = valueDescriptor.name.asString()
                    Name.identifier("$" + oldNameStr.substring(1, oldNameStr.length - 1))
                }
                else
                    valueDescriptor.name

        private fun createUnsubstitutedCapturedValueParameter(
                newParameterOwner: CallableMemberDescriptor,
                valueDescriptor: ValueDescriptor,
                index: Int
        ): ValueParameterDescriptor =
                ValueParameterDescriptorImpl(
                        newParameterOwner, null, index,
                        valueDescriptor.annotations,
                        suggestNameForCapturedValueParameter(valueDescriptor),
                        valueDescriptor.type,
                        false, false, false, null, valueDescriptor.source
                )

        private fun createUnsubstitutedParameter(
                newParameterOwner: CallableMemberDescriptor,
                valueParameterDescriptor: ValueParameterDescriptor,
                newIndex: Int
        ): ValueParameterDescriptor =
                valueParameterDescriptor.copy(newParameterOwner, valueParameterDescriptor.name, newIndex)


        private fun collectClosures() {
            memberFunction.acceptChildrenVoid(object : AbstractClosureAnnotator() {
                override fun visitClass(declaration: IrClass) {
                    // ignore local classes for now
                    return
                }

                override fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure) {
                    localFunctions[functionDescriptor]?.closure = closure
                }

                override fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure) {
                    // ignore local classes for now
                }
            })
        }

        private fun collectLocalFunctions() {
            memberFunction.acceptChildrenVoid(object : IrElementVisitorVoid {
                var lambdasCount = 0

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.acceptChildrenVoid(this)
                    val localFunctionContext = LocalFunctionContext(declaration)
                    localFunctions[declaration.descriptor] = localFunctionContext
                    if (declaration.descriptor.name.isSpecial) {
                        localFunctionContext.index = lambdasCount++
                    }
                }

                override fun visitClass(declaration: IrClass) {
                    // ignore local classes for now
                }
            })
        }
    }

}