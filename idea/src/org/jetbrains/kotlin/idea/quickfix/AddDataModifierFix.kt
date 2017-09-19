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
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class AddDataModifierFix(element: KtClass, private val fqName: String) : AddModifierFix(element, KtTokens.DATA_KEYWORD) {

    override fun getText() = "Make '$fqName' data class"

    override fun getFamilyName() = text

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtModifierListOwner>? {
            val element = diagnostic.psiElement as? KtExpression ?: return null

            val callableDescriptor = if (element is KtDestructuringDeclarationEntry)
                (element.parent.parent as? KtCallableDeclaration)?.descriptor as? CallableDescriptor
            else
                element.getCallableDescriptor()

            val constructor = callableDescriptor?.returnType?.arguments?.firstOrNull()?.type?.constructor
                              ?: callableDescriptor?.returnType?.constructor

            val classDescriptor = constructor?.declarationDescriptor as? ClassDescriptor ?: return null

            val modality = classDescriptor.modality
            if (modality == Modality.ABSTRACT || modality == Modality.SEALED || modality == Modality.OPEN) return null
            if (classDescriptor.isInner) return null
            val ctorParams = classDescriptor.constructors.firstOrNull { it.isPrimary }?.valueParameters ?: return null
            if (ctorParams.isEmpty()) return null

            val isSameContainingClass by lazy {
                classDescriptor ==  element.getTopmostParentOfType<KtClass>()?.descriptor
            }
            val isSameModule by lazy {
                classDescriptor.module == element.findModuleDescriptor()
            }
            if (!ctorParams.all {
                val param = DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtParameter ?: return@all false
                val isVisible = when {
                    param.hasModifier(KtTokens.PRIVATE_KEYWORD) || param.hasModifier(KtTokens.PROTECTED_KEYWORD) -> isSameContainingClass
                    param.hasModifier(KtTokens.INTERNAL_KEYWORD) -> isSameModule
                    else -> true
                }
                param.hasValOrVar() && !param.isVarArg && isVisible
            }) return null

            val klass = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor) as? KtClass ?: return null
            val fqName = DescriptorUtils.getFqName(classDescriptor).asString()
            return AddDataModifierFix(klass, fqName)
        }

    }

}
