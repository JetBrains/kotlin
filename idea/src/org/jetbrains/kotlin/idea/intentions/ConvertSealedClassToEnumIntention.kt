/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

class ConvertSealedClassToEnumIntention : SelfTargetingRangeIntention<KtClass>(
    KtClass::class.java,
    KotlinBundle.lazyMessage("convert.to.enum.class")
) {
    override fun applicabilityRange(element: KtClass): TextRange? {
        val nameIdentifier = element.nameIdentifier ?: return null
        val sealedKeyword = element.modifierList?.getModifier(KtTokens.SEALED_KEYWORD) ?: return null

        val classDescriptor = element.resolveToDescriptorIfAny() ?: return null
        if (classDescriptor.getSuperClassNotAny() != null) return null

        return TextRange(sealedKeyword.startOffset, nameIdentifier.endOffset)
    }

    override fun applyTo(element: KtClass, editor: Editor?) {
        val project = element.project

        val klass = element.liftToExpected() as? KtClass ?: element

        val subclasses: List<PsiElement> = project.runSynchronouslyWithProgress(KotlinBundle.message("searching.inheritors"), true) {
            HierarchySearchRequest(klass, klass.useScope, false).searchInheritors().mapNotNull { it.unwrapped }
        } ?: return

        val subclassesByContainer: Map<KtClass?, List<PsiElement>> = subclasses.groupBy {
            if (it !is KtObjectDeclaration) return@groupBy null
            if (it.superTypeListEntries.size != 1) return@groupBy null
            val containingClass = it.containingClassOrObject as? KtClass ?: return@groupBy null
            if (containingClass != klass && containingClass.liftToExpected() != klass) return@groupBy null
            containingClass
        }

        val inconvertibleSubclasses: List<PsiElement> = subclassesByContainer[null] ?: emptyList()
        if (inconvertibleSubclasses.isNotEmpty()) {
            return showError(
                KotlinBundle.message("all.inheritors.must.be.nested.objects.of.the.class.itself.and.may.not.inherit.from.other.classes.or.interfaces"),
                inconvertibleSubclasses,
                project,
                editor
            )
        }

        @Suppress("UNCHECKED_CAST")
        val nonSealedClasses = (subclassesByContainer.keys as Set<KtClass>).filter { !it.isSealed() }
        if (nonSealedClasses.isNotEmpty()) {
            return showError(
                KotlinBundle.message("all.expected.and.actual.classes.must.be.sealed.classes"),
                nonSealedClasses,
                project,
                editor
            )
        }

        if (subclassesByContainer.isNotEmpty()) {
            subclassesByContainer.forEach { (currentClass, currentSubclasses) ->
                processClass(currentClass!!, currentSubclasses, project)
            }
        } else {
            processClass(klass, emptyList(), project)
        }
    }

    private fun showError(message: String, elements: List<PsiElement>, project: Project, editor: Editor?) {
        val elementDescriptions = elements.map {
            ElementDescriptionUtil.getElementDescription(it, RefactoringDescriptionLocation.WITHOUT_PARENT)
        }

        val errorText = buildString {
            append(message)
            append(KotlinBundle.message("following.problems.are.found"))
            elementDescriptions.sorted().joinTo(this)
        }

        return CommonRefactoringUtil.showErrorHint(project, editor, errorText, text, null)
    }

    private fun processClass(klass: KtClass, subclasses: List<PsiElement>, project: Project) {
        val needSemicolon = klass.declarations.size > subclasses.size
        val movedDeclarations = run {
            val subclassesSet = subclasses.toSet()
            klass.declarations.filter { it in subclassesSet }
        }

        val psiFactory = KtPsiFactory(klass)

        val comma = psiFactory.createComma()
        val semicolon = psiFactory.createSemicolon()

        val constructorCallNeeded = klass.hasExplicitPrimaryConstructor() || klass.secondaryConstructors.isNotEmpty()

        val entriesToAdd = movedDeclarations.mapIndexed { i, subclass ->
            subclass as KtObjectDeclaration

            val entryText = buildString {
                append(subclass.name)
                if (constructorCallNeeded) {
                    append((subclass.superTypeListEntries.firstOrNull() as? KtSuperTypeCallEntry)?.valueArgumentList?.text ?: "()")
                }
            }

            val entry = psiFactory.createEnumEntry(entryText)
            subclass.body?.let { body -> entry.add(body) }

            if (i < movedDeclarations.lastIndex) {
                entry.add(comma)
            } else if (needSemicolon) {
                entry.add(semicolon)
            }

            entry
        }

        movedDeclarations.forEach { it.delete() }

        klass.removeModifier(KtTokens.SEALED_KEYWORD)
        klass.addModifier(KtTokens.ENUM_KEYWORD)

        if (entriesToAdd.isNotEmpty()) {
            val firstEntry = entriesToAdd
                .reversed()
                .asSequence()
                .map { klass.addDeclarationBefore(it, null) }
                .last()
            // TODO: Add formatter rule
            firstEntry.parent.addBefore(psiFactory.createNewLine(), firstEntry)
        } else if (needSemicolon) {
            klass.declarations.firstOrNull()?.let { anchor ->
                val delimiter = anchor.parent.addBefore(semicolon, anchor)
                CodeStyleManager.getInstance(project).reformat(delimiter)
            }
        }
    }
}