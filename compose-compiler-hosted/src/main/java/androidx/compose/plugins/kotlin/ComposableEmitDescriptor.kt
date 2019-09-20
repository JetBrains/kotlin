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
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ComposableEmitDescriptor(
    val ktxCall: ResolvedKtxElementCall,
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
): SimpleFunctionDescriptorImpl(
    containingDeclaration,
    original,
    annotations,
    name,
    kind,
    source
) {

    companion object {
        fun fromKtxCall(
            ktxCall: ResolvedKtxElementCall,
            scopeTower: ImplicitScopeTower,
            name: Name
        ): ComposableEmitDescriptor? {

            val builtIns = DefaultBuiltIns.Instance
            val emitOrCall = ktxCall.emitOrCall
            if (emitOrCall !is EmitCallNode) {
                return null
            }

            val resolvedCall = emitOrCall.primaryCall ?: return null

            val original = resolvedCall.candidateDescriptor
                            as? SimpleFunctionDescriptor
            val descriptor = ComposableEmitDescriptor(
                ktxCall,
                ktxCall.infixOrCall!!.candidateDescriptor.containingDeclaration,
                original,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            )

            val valueArgs = mutableListOf<ValueParameterDescriptor>()

            ktxCall.usedAttributes.forEachIndexed { index, attributeInfo ->
                valueArgs.add(
                    ValueParameterDescriptorImpl(
                        descriptor, null, index,
                        Annotations.EMPTY,
                        Name.identifier(
                            if (attributeInfo.name == CHILDREN_KEY)
                                attributeInfo.descriptor.name.identifier
                            else attributeInfo.name
                        ),
                        attributeInfo.type, false,
                        false,
                        false, null,
                        SourceElement.NO_SOURCE
                    )
                )
            }

            val unitLambdaType = builtIns.getFunction(
                0
            ).defaultType.replace(
                listOf(builtIns.unitType.asTypeProjection())
            ).makeComposable(scopeTower.module)
            (emitOrCall as? EmitCallNode)?.inlineChildren?.let {
                valueArgs.add(
                    ValueParameterDescriptorImpl(
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