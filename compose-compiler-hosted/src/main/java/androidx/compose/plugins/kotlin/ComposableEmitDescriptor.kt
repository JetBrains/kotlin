/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

interface ComposableEmitMetadata {
    val composerMetadata: ComposerMetadata
    val emitCall: ResolvedCall<*>
    val hasChildren: Boolean
    val pivotals: List<String>
    val ctorCall: ResolvedCall<*>
    val ctorParams: List<String>
    val validations: List<ValidatedAssignment>
}

class ComposableEmitDescriptor(
    val composer: ResolvedCall<*>,
    override val composerMetadata: ComposerMetadata,
    override val emitCall: ResolvedCall<*>,
    override val hasChildren: Boolean,
    override val pivotals: List<String>,
    override val ctorCall: ResolvedCall<*>,
    override val ctorParams: List<String>,
    override val validations: List<ValidatedAssignment>,
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : ComposableEmitMetadata, SimpleFunctionDescriptorImpl(
    containingDeclaration,
    original,
    annotations,
    name,
    kind,
    source
) {

    companion object {
        fun build(
            hasChildren: Boolean,
            emitCall: ResolvedCall<*>,
            pivotals: List<String>,
            ctorCall: ResolvedCall<*>,
            ctorParams: List<String>,
            validations: List<ValidatedAssignment>,
            composerCall: ResolvedCall<*>,
            composerMetadata: ComposerMetadata,
            name: Name
        ): ComposableEmitDescriptor {

            val builtIns = DefaultBuiltIns.Instance

            val resolvedCall = ctorCall

            val original = resolvedCall.resultingDescriptor as? SimpleFunctionDescriptor

            val descriptor = ComposableEmitDescriptor(
                composerCall,
                composerMetadata,
                emitCall,
                hasChildren,
                pivotals,
                ctorCall,
                ctorParams,
                validations,
                emitCall.candidateDescriptor.containingDeclaration,
                original,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            )

            val valueArgs = mutableListOf<ValueParameterDescriptor>()
            val paramSet = mutableSetOf<String>()

            for (paramName in ctorParams) {
                if (paramSet.contains(paramName)) continue
                val param = resolvedCall.resultingDescriptor.valueParameters.find {
                    it.name.identifier == paramName
                } ?: continue

                paramSet.add(paramName)
                valueArgs.add(
                    ValueParameterDescriptorImpl(
                        descriptor, null, valueArgs.size,
                        Annotations.EMPTY,
                        param.name,
                        param.type, false,
                        false,
                        false, null,
                        SourceElement.NO_SOURCE
                    )
                )
            }

            for (validation in validations) {
                if (paramSet.contains(validation.name)) continue
                paramSet.add(validation.name)
                valueArgs.add(
                    ValueParameterDescriptorImpl(
                        descriptor,
                        null,
                        valueArgs.size,
                        Annotations.EMPTY,
                        Name.identifier(validation.name),
                        validation.type,
                        false,
                        false,
                        false,
                        null,
                        SourceElement.NO_SOURCE
                    )
                )
            }

            val unitLambdaType = builtIns.getFunction(
                0
            ).defaultType.replace(
                listOf(builtIns.unitType.asTypeProjection())
            )
            // NOTE(lmr): it's actually kind of important that this is *not* a composable lambda,
            // so that the observe patcher doesn't insert an observe scope.
            // In the future, we should reconsider how this is done since semantically a composable
            // lambda is more correct here. I tried, but had trouble passing enough information to
            // the observe patcher so it knew not to do this.
            /*.makeComposable(scopeTower.module)*/
            if (hasChildren) {
                valueArgs.add(
                    EmitChildrenValueParameterDescriptor(
                        descriptor, null, valueArgs.size,
                        Annotations.EMPTY,
                        Name.identifier("\$CHILDREN"),
                        unitLambdaType, false,
                        false,
                        false, null,
                        SourceElement.NO_SOURCE
                    )
                )
            }

            descriptor.initialize(
                null,
                null,
                mutableListOf(),
                valueArgs,
                builtIns.unitType,
                Modality.FINAL,
                Visibilities.DEFAULT_VISIBILITY
            )

            return descriptor
        }
    }
}

class EmitChildrenValueParameterDescriptor(
    containingDeclaration: CallableDescriptor,
    original: ValueParameterDescriptor?,
    index: Int,
    annotations: Annotations,
    name: Name,
    outType: KotlinType,
    declaresDefaultValue: Boolean,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    varargElementType: KotlinType?,
    source: SourceElement
) : ValueParameterDescriptorImpl(
    containingDeclaration,
    original,
    index,
    annotations,
    name,
    outType,
    declaresDefaultValue,
    isCrossinline,
    isNoinline,
    varargElementType,
    source
)