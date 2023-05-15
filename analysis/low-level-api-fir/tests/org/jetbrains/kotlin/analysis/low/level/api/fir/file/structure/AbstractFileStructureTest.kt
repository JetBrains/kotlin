/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.isSourceSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFileStructureTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val fileStructure = ktFile.getFileStructure()
        val allStructureElements = fileStructure.getAllStructureElements(ktFile)
        val declarationToStructureElement = allStructureElements.associateBy { it.psi }

        val elementToComment = mutableMapOf<PsiElement, String>()
        ktFile.forEachDescendantOfType<KtDeclaration> { ktDeclaration ->
            val structureElement = declarationToStructureElement[ktDeclaration] ?: return@forEachDescendantOfType
            val comment = structureElement.createComment()
            when (ktDeclaration) {
                is KtClassOrObject -> {
                    val body = ktDeclaration.body
                    val lBrace = body?.lBrace
                    if (lBrace != null) {
                        elementToComment[lBrace] = comment
                    } else {
                        elementToComment[ktDeclaration] = comment
                    }
                }
                is KtFunction -> {
                    val lBrace = ktDeclaration.bodyBlockExpression?.lBrace
                    if (lBrace != null) {
                        elementToComment[lBrace] = comment
                    } else {
                        elementToComment[ktDeclaration] = comment
                    }
                }
                is KtProperty -> {
                    val initializerOrTypeReference = ktDeclaration.initializer ?: ktDeclaration.typeReference
                    if (initializerOrTypeReference != null) {
                        elementToComment[initializerOrTypeReference] = comment
                    } else {
                        elementToComment[ktDeclaration] = comment
                    }
                }
                is KtTypeAlias -> {
                    elementToComment[ktDeclaration.getTypeReference()!!] = comment
                }
                is KtClassInitializer -> {
                    elementToComment[ktDeclaration.openBraceNode!!] = comment
                }
                else -> error("Unsupported declaration $ktDeclaration")
            }
        }

        PsiTreeUtil.getChildrenOfTypeAsList(ktFile, KtModifierList::class.java).forEach {
            if (it.nextSibling is PsiErrorElement) {
                val structureElement = declarationToStructureElement[ktFile] ?: return@forEach
                val comment = structureElement.createComment()
                elementToComment[it] = comment
            }
        }

        val text = buildString {
            ktFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is LeafPsiElement) {
                        append(element.text)
                    }
                    element.acceptChildren(this)
                    elementToComment[element]?.let {
                        append(it)
                    }
                }

                override fun visitComment(comment: PsiComment) {}
            })
        }

        KotlinTestUtils.assertEqualsToFile(testDataPath, text)
    }

    private fun FileStructureElement.createComment(): String {
        return """/* ${this::class.simpleName!!} */"""
    }

    private fun KtFile.getFileStructure(): FileStructure {
        val module = ProjectStructureProvider.getModule(project, this, contextualModule = null)
        val moduleFirResolveSession = module.getFirResolveSession(project)
        check(moduleFirResolveSession.isSourceSession)
        val session = moduleFirResolveSession.getSessionFor(module) as LLFirResolvableModuleSession
        return session.moduleComponents.fileStructureCache.getFileStructure(this)
    }

    private fun FileStructure.getAllStructureElements(ktFile: KtFile): Collection<FileStructureElement> = buildSet {
        ktFile.forEachDescendantOfType<KtElement> { ktElement ->
            add(getStructureElementFor(ktElement))
        }
    }
}

abstract class AbstractSourceFileStructureTest : AbstractFileStructureTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootFileStructureTest : AbstractFileStructureTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}