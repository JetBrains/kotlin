/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.isSourceSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.services.TestServices

/**
 * The test visualizes the [FileStructure] structure in human-readable form
 */
abstract class AbstractFileStructureTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val fileStructure = mainFile.getFileStructure()
        val allStructureElements = fileStructure.getAllStructureElements(mainFile)
        val elementsToStructureElementMap = allStructureElements.associateBy { it.declaration.psi }
        val elementToComments = elementsToStructureElementMap.entries.fold(
            initial = listMultimapOf<PsiElement, String>()
        ) { map, (anchorElement, structureElement) ->
            val specialKey: PsiElement? = when (anchorElement) {
                is KtClassOrObject -> anchorElement.body?.lBrace
                is KtFunction -> anchorElement.bodyBlockExpression?.lBrace
                is KtProperty -> anchorElement.initializer ?: anchorElement.typeReference
                is KtTypeAlias -> anchorElement.getTypeReference()!!
                is KtClassInitializer -> anchorElement.openBraceNode!!
                is KtScript -> mainFile.importList!!
                is KtFile -> anchorElement.packageDirective ?: anchorElement.importList
                is KtScriptInitializer -> anchorElement.body!!
                is KtDestructuringDeclaration, is KtDestructuringDeclarationEntry, is KtModifierList -> null
                else -> error("Unsupported declaration ${anchorElement?.let { it::class.simpleName }}")
            }

            map.apply {
                put(key = specialKey ?: anchorElement, value = structureElement.createComment())
            }
        }

        val anchorElements = elementsToStructureElementMap.keys.toMutableSet()

        val text = buildString {
            mainFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    anchorElements -= element

                    if (element is LeafPsiElement) {
                        append(element.text)
                    }

                    element.acceptChildren(this)
                    elementToComments[element].forEach(this@buildString::append)
                }

                override fun visitComment(comment: PsiComment) {}
            })
        }

        KotlinTestUtils.assertEqualsToFile(testDataPath, text)

        if (anchorElements.isNotEmpty()) {
            error(
                "An anchor element is not found in the file:\n" +
                        anchorElements.joinToString(separator = "\n") { element ->
                            element?.let { it::class.simpleName }.toString()
                        }
            )
        }
    }

    private fun FileStructureElement.createComment(): String {
        return """/* ${this::class.simpleName!!} */"""
    }

    private fun KtFile.getFileStructure(): FileStructure {
        val module = KotlinProjectStructureProvider.getModule(project, this, useSiteModule = null)
        val resolutionFacade = module.getResolutionFacade(project)
        check(resolutionFacade.isSourceSession)
        val session = resolutionFacade.getSessionFor(module) as LLFirResolvableModuleSession
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
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptFileStructureTest : AbstractFileStructureTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}