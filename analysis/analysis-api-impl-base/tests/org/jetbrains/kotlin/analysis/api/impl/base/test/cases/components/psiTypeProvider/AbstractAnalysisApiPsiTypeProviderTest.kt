/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

abstract class AbstractAnalysisApiPsiTypeProviderTest : AbstractAnalysisApiSingleFileTest(){
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypAtCaret<KtDeclaration>(ktFile)
        val containingClass =
            getContainingKtLightClass(declaration, ktFile)
        val psiContext = containingClass.findLightDeclarationContext(declaration)
            ?: error("Can't find psi context for $declaration")
        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyse(declaration) {
                    val ktType = declaration.getReturnKtType()
                    appendLine("KtType: ${ktType.render()}")
                    appendLine("PsiType: ${ktType.asPsiType(psiContext)}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun getContainingKtLightClass(
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
    ): KtLightClass? {
        val mainKtFileFqName = ktFile.packageFqName.child(Name.identifier(ktFile.name))
        return project.getService(SymbolLightClassFacadeCache::class.java)
            .getOrCreateSymbolLightFacade(listOf(ktFile), mainKtFileFqName)
    }

    private fun createLightClassByContainingClass(
        declaration: KtDeclaration,
        project: Project
    ): KtLightClass? {
        val containingClass = declaration.parents.firstIsInstanceOrNull<KtClassOrObject>() ?: return null
        return project.getService(KotlinAsJavaSupport::class.java).getLightClass(containingClass)
    }

    private fun KtLightClass.findLightDeclarationContext(ktDeclaration: KtDeclaration): KtLightElement<*, *>? {
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
