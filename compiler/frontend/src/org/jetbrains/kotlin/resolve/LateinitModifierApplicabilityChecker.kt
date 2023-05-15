/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DeclarationsChecker.Companion.hasAnyAccessorImplementation
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isUnsignedNumberType

object LateinitModifierApplicabilityChecker {
    fun checkLateinitModifierApplicability(
        trace: BindingTrace,
        ktDeclaration: KtCallableDeclaration,
        descriptor: VariableDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (!ktDeclaration.hasModifier(KtTokens.LATEINIT_KEYWORD)) return

        val variables = when (descriptor) {
            is PropertyDescriptor -> "properties"
            is LocalVariableDescriptor -> "local variables"
            else -> throw AssertionError("Should be a property or a local variable: $descriptor")
        }

        val type = descriptor.type

        if (!descriptor.isVar) {
            trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is allowed only on mutable $variables"))
        }

        if (type.isInlineClassType()) {
            when {
                type.isUnsignedNumberType() -> trace.report(
                    Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                        ktDeclaration,
                        "is not allowed on $variables of unsigned types"
                    )
                )
                !languageVersionSettings.supportsFeature(LanguageFeature.InlineLateinit) ->
                    trace.report(
                        Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                            ktDeclaration,
                            "is not allowed on $variables of inline class types"
                        )
                    )
                hasUnderlyingTypeForbiddenForLateinit(type) ->
                    trace.report(
                        Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                            ktDeclaration,
                            "is not allowed on $variables of inline type with underlying type not suitable for lateinit declaration"
                        )
                    )
            }
        }

        if (type.isMarkedNullable) {
            trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on $variables of nullable types"))
        } else if (TypeUtils.isNullableType(type)) {
            trace.report(
                Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                    ktDeclaration,
                    "is not allowed on $variables of a type with nullable upper bound"
                )
            )
        }

        if (KotlinBuiltIns.isPrimitiveType(type)) {
            trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on $variables of primitive types"))
        }

        if (ktDeclaration is KtProperty) {
            if (ktDeclaration.hasDelegateExpression()) {
                trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on delegated properties"))
            } else if (ktDeclaration.hasInitializer()) {
                trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on $variables with initializer"))
            }
        }

        if (descriptor is PropertyDescriptor) {
            val isAbstract = descriptor.modality == Modality.ABSTRACT
            val hasDelegateExpressionOrInitializer = ktDeclaration is KtProperty && ktDeclaration.hasDelegateExpressionOrInitializer()
            val hasAccessorImplementation = descriptor.hasAnyAccessorImplementation()
            val hasBackingField = trace.bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) ?: false

            if (ktDeclaration is KtParameter) {
                trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on primary constructor parameters"))
            }

            if (isAbstract) {
                trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on abstract properties"))
            }

            if (!hasDelegateExpressionOrInitializer) {
                if (hasAccessorImplementation) {
                    trace.report(
                        Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                            ktDeclaration,
                            "is not allowed on properties with a custom getter or setter"
                        )
                    )
                } else if (!isAbstract && !hasBackingField) {
                    trace.report(
                        Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(
                            ktDeclaration,
                            "is not allowed on properties without backing field"
                        )
                    )
                }
            }

            if (descriptor.extensionReceiverParameter != null) {
                trace.report(Errors.INAPPLICABLE_LATEINIT_MODIFIER.on(ktDeclaration, "is not allowed on extension properties"))
            }
        }
    }

    private fun hasUnderlyingTypeForbiddenForLateinit(type: KotlinType): Boolean {

        fun getUnderlyingType(type: KotlinType): KotlinType {
            return (type.constructor.declarationDescriptor as ClassDescriptor).inlineClassRepresentation!!.underlyingType
        }

        fun isForbiddenForLateinit(type: KotlinType): Boolean {
            if (type.isMarkedNullable || TypeUtils.isNullableType(type)) return true
            if (KotlinBuiltIns.isPrimitiveType(type)) return true
            if (type.isInlineClassType()) {
                return isForbiddenForLateinit(getUnderlyingType(type))
            }
            return false
        }

        // prevent infinite recursion
        if (type.isRecursiveInlineOrValueClassType()) return false
        return isForbiddenForLateinit(getUnderlyingType(type))
    }


}