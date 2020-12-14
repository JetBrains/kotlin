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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmInlineApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        val annotation = descriptor.annotations.findAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)
        if (annotation != null && !descriptor.isValue) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
            context.trace.report(ErrorsJvm.JVM_INLINE_WITHOUT_VALUE_CLASS.on(annotationEntry))
        }

        if (descriptor.isValue && annotation == null) {
            val valueKeyword = declaration.modifierList?.getModifier(KtTokens.VALUE_KEYWORD) ?: return
            context.trace.report(ErrorsJvm.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION.on(valueKeyword))
        }
    }
}
