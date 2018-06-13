/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.sealedSubClassToObject

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ConvertSealedSubClassToObjectFix : LocalQuickFix {

    override fun getFamilyName() = "Convert sealed sub-class to object"

    companion object {
        val JAVA_LANG = Language.findLanguageByID("JAVA")
        val KOTLIN_LANG = Language.findLanguageByID("kotlin")
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val klass = descriptor.psiElement.getParentOfType<KtClass>(false) ?: return

        changeInstances(klass)
        changeDeclaration(klass)
    }

    /**
     * Changes declaration of class to object.
     */
    private fun changeDeclaration(element: KtClass) {
        val factory = KtPsiFactory(element)

        element.changeToObject(factory)
        element.transformToObject(factory)
    }

    private fun KtClass.changeToObject(factory: KtPsiFactory) {
        getClassOrInterfaceKeyword()?.replace(factory.createExpression(KtTokens.OBJECT_KEYWORD.value))
        secondaryConstructors.forEach { delete() }
        primaryConstructor?.delete()
    }

    private fun KtClass.transformToObject(factory: KtPsiFactory) {
        replace(factory.createObject(text))
    }

    /**
     * Replace instantiations of the class with links to the singleton instance of the object.
     */
    private fun changeInstances(klass: KtClass) {
        mapReferencesByLanguage(klass)
            .apply {
                replaceKotlin(klass)
                replaceJava(klass)
            }
    }

    /**
     * Map references to this class by language
     */
    private fun mapReferencesByLanguage(klass: KtClass) = ReferencesSearch.search(klass)
        .groupBy({ it.element.language }, { it.element.parent })

    /**
     * Replace Kotlin instantiations to a straightforward call to the singleton.
     */
    private fun Map<Language, List<PsiElement>>.replaceKotlin(klass: KtClass) {
        val list = this[KOTLIN_LANG] ?: return
        val singletonCall = KtPsiFactory(klass).buildExpression { appendName(klass.nameAsSafeName) }

        list.filter { it.node.elementType == KtNodeTypes.CALL_EXPRESSION }
            .forEach { it.replace(singletonCall) }
    }

    /**
     * Replace Java instantiations to an instance of the object, unless it is the only thing
     * done in the statement, in which IDEA will consider wrong, so I delete the line.
     */
    private fun Map<Language, List<PsiElement>>.replaceJava(klass: KtClass) {
        val list = this[JAVA_LANG] ?: return
        val first = list.firstOrNull() ?: return
        val elementFactory = JavaPsiFacade.getElementFactory(klass.project)
        val javaSingletonCall = elementFactory.createExpressionFromText("${klass.name}.INSTANCE", first)

        list.filter { it.node.elementType == JavaElementType.NEW_EXPRESSION }
            .forEach {
                when (it.parent.node.elementType) {
                    JavaElementType.EXPRESSION_STATEMENT -> it.delete()
                    else -> it.replace(javaSingletonCall)
                }
            }
    }
}