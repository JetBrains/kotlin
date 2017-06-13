/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.types.KotlinType

class SignatureEnhancement {
    fun <D : CallableMemberDescriptor> enhanceSignatures(platformSignatures: Collection<D>): Collection<D> {
        return platformSignatures.map {
            it.enhanceSignature()
        }
    }

    private fun <D : CallableMemberDescriptor> D.enhanceSignature(): D {
        // TODO type parameters
        // TODO use new type parameters while enhancing other types
        // TODO Propagation into generic type arguments

        if (this !is JavaCallableMemberDescriptor) return this

        // Fake overrides with one overridden has been enhanced before
        if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE && original.overriddenDescriptors.size == 1) return this

        val receiverTypeEnhancement =
                if (extensionReceiverParameter != null)
                    parts(isCovariant = false) { it.extensionReceiverParameter!!.type }.enhance()
                else null


        val predefinedEnhancementInfo =
                (this as? JavaMethodDescriptor)
                        ?.run { SignatureBuildingComponents.signature(this.containingDeclaration as ClassDescriptor, this.computeJvmDescriptor()) }
                        ?.let { signature -> PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature] }


        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${valueParameters.size} expected"
            }
        }

        val valueParameterEnhancements = valueParameters.map {
            p ->
            parts(isCovariant = false) { it.valueParameters[p.index].type }
                    .enhance(predefinedEnhancementInfo?.parametersInfo?.getOrNull(p.index))
        }

        val returnTypeEnhancement = parts(isCovariant = true) { it.returnType!! }.enhance(predefinedEnhancementInfo?.returnTypeInfo)

        if ((receiverTypeEnhancement?.wereChanges ?: false)
            || returnTypeEnhancement.wereChanges || valueParameterEnhancements.any { it.wereChanges }) {
            @Suppress("UNCHECKED_CAST")
            return this.enhance(receiverTypeEnhancement?.type, valueParameterEnhancements.map { it.type }, returnTypeEnhancement.type) as D
        }

        return this
    }

    private class SignatureParts(
            val fromOverride: KotlinType,
            val fromOverridden: Collection<KotlinType>,
            val isCovariant: Boolean
    ) {
        fun enhance(predefined: TypeEnhancementInfo? = null): PartEnhancementResult {
            val qualifiers = fromOverride.computeIndexedQualifiersForOverride(this.fromOverridden, isCovariant)

            val qualifiersWithPredefined: ((Int) -> JavaTypeQualifiers)? = predefined?.let {
                {
                    index ->
                    predefined.map[index] ?: qualifiers(index)
                }
            }

            return fromOverride.enhance(qualifiersWithPredefined ?: qualifiers)?.let {
                enhanced ->
                PartEnhancementResult(enhanced, wereChanges = true)
            } ?: PartEnhancementResult(fromOverride, wereChanges = false)
        }
    }

    private data class PartEnhancementResult(val type: KotlinType, val wereChanges: Boolean)

    private fun <D : CallableMemberDescriptor> D.parts(isCovariant: Boolean, collector: (D) -> KotlinType): SignatureParts {
        return SignatureParts(
                collector(this),
                this.overriddenDescriptors.map {
                    @Suppress("UNCHECKED_CAST")
                    collector(it as D)
                },
                isCovariant
        )
    }
}
