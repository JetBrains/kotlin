/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal object JavaInteroperabilityComponentTestUtils {
    fun KaType.render(analysisSession: KaSession): String {
        with(analysisSession) {
            return this@render.render(position = Variance.INVARIANT)
        }
    }

    fun PsiType.render(): String = PsiClassRenderer.renderType(this)

    internal fun getContainingKtLightClass(
        declaration: KtDeclaration,
        ktFile: KtFile,
    ): KtLightClass {
        val project = ktFile.project
        return createLightClassByContainingClass(declaration, project)
            ?: getFacadeLightClass(ktFile, project)
            ?: error("Can't get or create containing KtLightClass for $declaration")

    }

    private fun getFacadeLightClass(
        ktFile: KtFile,
        project: Project,
    ): KtLightClass? = project.getService(KotlinAsJavaSupport::class.java).getLightFacade(ktFile)

    private fun createLightClassByContainingClass(
        declaration: KtDeclaration,
        project: Project
    ): KtLightClass? {
        val containingClass = declaration.parents.firstIsInstanceOrNull<KtClassOrObject>() ?: return null
        return KotlinAsJavaSupport.getInstance(project).getLightClass(containingClass)
    }

    internal fun KtLightClass.findLightDeclarationContext(ktDeclaration: KtDeclaration): KtLightElement<*, *>? {
        val selfOrParents = listOf(ktDeclaration) + ktDeclaration.parents.filterIsInstance<KtDeclaration>()
        var result: KtLightElement<*, *>? = null
        val visitor = object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is KtLightElement<*, *>) return
                // NB: intentionally visit members first so that `self` can be found first if matched
                if (element is PsiClass) {
                    element.fields.forEach { it.accept(this) }
                    element.methods.forEach { it.accept(this) }
                    element.innerClasses.forEach { it.accept(this) }
                }
                if (result == null && element.kotlinOrigin in selfOrParents) {
                    result = element
                    return
                }
            }
        }
        accept(visitor)
        return result
    }
}