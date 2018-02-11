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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.highlighter.markers.liftToExpected
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

class ConvertSealedClassToEnumIntention : SelfTargetingRangeIntention<KtClass>(KtClass::class.java, "Convert to enum class") {
    override fun applicabilityRange(element: KtClass): TextRange? {
        val nameIdentifier = element.nameIdentifier ?: return null
        val sealedKeyword = element.modifierList?.getModifier(KtTokens.SEALED_KEYWORD) ?: return null

        val classDescriptor = element.resolveToDescriptorIfAny() as? ClassDescriptor ?: return null
        if (classDescriptor.getSuperClassNotAny() != null) return null

        return TextRange(sealedKeyword.startOffset, nameIdentifier.endOffset)
    }

    override fun applyTo(element: KtClass, editor: Editor?) {
        val project = element.project

        val klass = element.liftToExpected() as? KtClass ?: element

        val subclasses = project.runSynchronouslyWithProgress("Searching inheritors...", true) {
            HierarchySearchRequest(klass, klass.useScope, false).searchInheritors().mapNotNull { it.unwrapped }
        } ?: return

        val subclassesByContainer = subclasses.groupBy {
            if (it !is KtObjectDeclaration) return@groupBy null
            if (it.superTypeListEntries.size != 1) return@groupBy null
            val containingClass = it.containingClassOrObject as? KtClass ?: return@groupBy null
            if (containingClass != klass && containingClass.liftToExpected() != klass) return@groupBy null
            containingClass
        }

        val inconvertibleSubclasses = subclassesByContainer[null] ?: emptyList()
        if (inconvertibleSubclasses.isNotEmpty()) {
            return showError(
                    "All inheritors must be nested objects of the class itself and may not inherit from other classes or interfaces.\n",
                    inconvertibleSubclasses,
                    project,
                    editor
            )
        }

        @Suppress("UNCHECKED_CAST")
        val nonSealedClasses = (subclassesByContainer.keys as Set<KtClass>).filter { !it.isSealed() }
        if (nonSealedClasses.isNotEmpty()) {
            return showError("All expected and actual classes must be sealed classes.\n", nonSealedClasses, project, editor)
        }

        if (subclassesByContainer.isNotEmpty()) {
            subclassesByContainer.forEach { currentClass, currentSubclasses -> processClass(currentClass!!, currentSubclasses, project) }
        }
        else {
            processClass(klass, emptyList(), project)
        }
    }

    private fun showError(message: String, elements: List<PsiElement>, project: Project, editor: Editor?) {
        val errorText = buildString {
            append(message)
            append("Following problems are found:\n")
            elements.joinTo(this) { ElementDescriptionUtil.getElementDescription(it, RefactoringDescriptionLocation.WITHOUT_PARENT) }
        }
        return CommonRefactoringUtil.showErrorHint(project, editor, errorText, text, null)
    }

    private fun processClass(klass: KtClass, subclasses: List<PsiElement>, project: Project) {
        val needSemicolon = klass.declarations.size > subclasses.size

        val psiFactory = KtPsiFactory(klass)

        val comma = psiFactory.createComma()
        val semicolon = psiFactory.createSemicolon()

        val constructorCallNeeded = klass.hasExplicitPrimaryConstructor() || klass.secondaryConstructors.isNotEmpty()
        val entriesToAdd = subclasses.mapIndexed { i, subclass ->
            subclass as KtObjectDeclaration

            val entryText = buildString {
                append(subclass.name)
                if (constructorCallNeeded) {
                    append((subclass.superTypeListEntries.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList?.text ?: "()")
                }
            }
            val entry = psiFactory.createEnumEntry(entryText)

            subclass.getBody()?.let { body -> entry.add(body) }

            if (i < subclasses.lastIndex) {
                entry.add(comma)
            }
            else if (needSemicolon) {
                entry.add(semicolon)
            }

            entry
        }

        subclasses.forEach { it.delete() }

        klass.removeModifier(KtTokens.SEALED_KEYWORD)
        klass.addModifier(KtTokens.ENUM_KEYWORD)

        if (entriesToAdd.isNotEmpty()) {
            val firstEntry = entriesToAdd
                    .reversed()
                    .map { klass.addDeclarationBefore(it, null) }
                    .last()
            // TODO: Add formatter rule
            firstEntry.parent.addBefore(psiFactory.createNewLine(), firstEntry)
        }
        else if (needSemicolon) {
            klass.declarations.firstOrNull()?.let { anchor ->
                val delimiter = anchor.parent.addBefore(semicolon, anchor)
                CodeStyleManager.getInstance(project).reformat(delimiter)
            }
        }
    }
}