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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.isSealedInlineClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class JvmInlineApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        val annotation = descriptor.annotations.findAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)
        if (annotation != null && !descriptor.isValueClass) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
            context.trace.report(ErrorsJvm.JVM_INLINE_WITHOUT_VALUE_CLASS.on(annotationEntry))
        }

        if (descriptor.isValueClass && annotation == null && !descriptor.isExpect &&
            !descriptor.getSuperClassOrAny().isSealedInlineClass()
        ) {
            val valueKeyword = declaration.modifierList?.getModifier(KtTokens.VALUE_KEYWORD) ?: return
            context.trace.report(ErrorsJvm.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION.on(valueKeyword))
        }

        if (descriptor.getSuperClassOrAny().isSealedInlineClass() && declaration is KtClass) {
            checkUnderlyingValueTypesDoNotIntersect(declaration, descriptor, descriptor.getSuperClassOrAny(), context)
        }
    }

    private fun checkUnderlyingValueTypesDoNotIntersect(
        declaration: KtClass,
        descriptor: ClassDescriptor,
        parent: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.annotations.hasAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)) return

        val baseParameterType = descriptor.defaultType.substitutedUnderlyingTypes().singleOrNull()
        val baseParameterTypeReference = declaration.primaryConstructor?.valueParameters?.singleOrNull()?.typeReference
        if (baseParameterType != null && baseParameterTypeReference != null) {
            // TODO: compute intersection types and check, that they are Nothing.
            val children = parent.sealedSubclasses.filter { it.isInlineClass() }
            for (child in children) {
                if (child == descriptor) continue
                val anotherType =
                    if (child.modality == Modality.SEALED) context.moduleDescriptor.builtIns.nullableAnyType
                    else child.defaultType.substitutedUnderlyingType()
                if (anotherType == null) continue
                if (baseParameterType.isSubtypeOf(anotherType)) {
                    context.trace.report(
                        ErrorsJvm.INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE.on(baseParameterTypeReference)
                    )
                    break
                }
            }
        } else if (descriptor.modality == Modality.SEALED) {
            if (parent.sealedSubclasses.any {
                    it.isSealedInlineClass() && it != descriptor
                }
            ) {
                val sealedKeyword = declaration.modifierList?.getModifier(KtTokens.SEALED_KEYWORD) ?: declaration
                context.trace.report(
                    ErrorsJvm.INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE.on(sealedKeyword)
                )
            }
        }
    }
}

val ClassDescriptor.isValueClass: Boolean
    get() = kind == ClassKind.CLASS && isValue
