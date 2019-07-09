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

package org.jetbrains.kotlin.idea.quickfix

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class AddDataModifierFix(element: KtClass, private val fqName: String) : AddModifierFix(element, KtTokens.DATA_KEYWORD) {

    override fun getText() = "Make '$fqName' data class"

    override fun getFamilyName() = text

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): AddDataModifierFix? {
            val element = diagnostic.psiElement as? KtExpression ?: return null
            val context = element.analyze()

            val callableDescriptor = if (element is KtDestructuringDeclarationEntry) {
                context[BindingContext.DECLARATION_TO_DESCRIPTOR, element.parent.parent] as? CallableDescriptor
            }
            else {
                element.getResolvedCall(context)?.resultingDescriptor
            }

            val constructor = callableDescriptor?.returnType?.arguments?.firstOrNull()?.type?.constructor
                              ?: callableDescriptor?.returnType?.constructor

            val classDescriptor = constructor?.declarationDescriptor as? ClassDescriptor ?: return null

            val modality = classDescriptor.modality
            if (modality != Modality.FINAL || classDescriptor.isInner) return null
            val ctorParams = classDescriptor.constructors.firstOrNull { it.isPrimary }?.valueParameters ?: return null
            if (ctorParams.isEmpty()) return null

            if (!ctorParams.all {
                if (it.varargElementType != null) return@all false
                val property = context[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it] ?: return@all false
                // NB: we use element as receiver because element is a constructor call
                // which is effectively used as receiver by destructuring declaration
                property.isVisible(element, element, context, element.getResolutionFacade())
            }) return null

            val klass = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor) as? KtClass ?: return null
            val fqName = DescriptorUtils.getFqName(classDescriptor).asString()
            return AddDataModifierFix(klass, fqName)
        }

    }

}
