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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.java.JavaFindUsagesProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.typeUtil.isUnit

class KotlinFindUsagesProvider : FindUsagesProvider {
    private val javaProvider by lazy { JavaFindUsagesProvider() }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
            psiElement is KtNamedDeclaration

    override fun getWordsScanner() = null

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when(element) {
            is KtNamedFunction -> "function"
            is KtClass -> "class"
            is KtParameter -> "parameter"
            is KtProperty -> if (element.isLocal) "variable" else "property"
            is KtDestructuringDeclarationEntry -> "variable"
            is KtTypeParameter -> "type parameter"
            is KtSecondaryConstructor -> "constructor"
            is KtObjectDeclaration -> "object"
            else -> ""
        }
    }

    private val KtDeclaration.containerDescription: String?
        get() {
            containingClassOrObject?.let { return getDescriptiveName(it) }
            (parent as? KtFile)?.parent?.let { return getDescriptiveName(it) }
            return null
        }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PsiDirectory, is PsiPackage, is PsiFile -> javaProvider.getDescriptiveName(element)
            is KtClassOrObject -> {
                if (element is KtObjectDeclaration && element.isObjectLiteral()) return "<unnamed>"
                element.fqName?.asString() ?: element.name ?: "<unnamed>"
            }
            is KtProperty -> (element.name ?: "") + (element.containerDescription?.let { " of $it" } ?: "")
            is KtFunction -> {
                val name = element.name ?: ""
                val descriptor = element.unsafeResolveToDescriptor() as FunctionDescriptor
                val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES
                val paramsDescription = descriptor.valueParameters.joinToString(prefix = "(", postfix = ")") { renderer.renderType(it.type) }
                val returnType = descriptor.returnType
                val returnTypeDescription = if (returnType != null && !returnType.isUnit()) renderer.renderType(returnType) else null
                val funDescription = "$name$paramsDescription" + (returnTypeDescription?.let { ": $it" } ?: "")
                return funDescription + (element.containerDescription?.let { " of $it" } ?: "")
            }
            is KtLabeledExpression -> element.getLabelName() ?: ""
            is KtImportAlias -> element.getName() ?: ""
            is KtLightElement<*, *> -> element.kotlinOrigin?.let { getDescriptiveName(it) } ?: ""
            else -> ""
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
            getDescriptiveName(element)
}
