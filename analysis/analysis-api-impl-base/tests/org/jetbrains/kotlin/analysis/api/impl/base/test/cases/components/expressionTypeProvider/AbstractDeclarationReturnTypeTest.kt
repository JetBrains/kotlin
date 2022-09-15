/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import com.intellij.codeInsight.BaseExternalAnnotationsManager
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.utils.getNameWithPositionString
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

class MockExternalAnnotationsManagerForPlatformTypeTest(psiManager: PsiManager) : BaseExternalAnnotationsManager(psiManager) {
    override fun hasAnyAnnotationsRoots(): Boolean = false

    override fun getExternalAnnotationsRoots(libraryFile: VirtualFile): MutableList<VirtualFile> = mutableListOf()

}

private const val ENABLE_EXTERNAL_ANNOTATION_MARKER = "FIR_ENABLE_EXTERNAL_ANNOTATION"

abstract class AbstractDeclarationReturnTypeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        if (doesFileContainMarker(ktFile, ENABLE_EXTERNAL_ANNOTATION_MARKER)) registerService(ktFile.project)
        val actual = buildString {
            ktFile.accept(object : KtTreeVisitor<Int>() {
                override fun visitDeclaration(declaration: KtDeclaration, indent: Int): Void? {
                    if (declaration is KtTypeParameter) return null
                    append(" ".repeat(indent))
                    if (declaration is KtClassLikeDeclaration) {
                        appendLine(declaration.getNameWithPositionString())
                    } else {
                        analyseForTest(declaration) {
                            val returnType = declaration.getReturnKtType()
                            append(declaration.getNameWithPositionString())
                            append(" : ")
                            appendLine(returnType.render())
                        }
                    }
                    return super.visitDeclaration(declaration, indent + 2)
                }
            }, 0)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun doesFileContainMarker(ktFile: KtFile, marker: String) =
        ktFile.context.toString().contains(marker)

    private fun registerService(project: Project) {
        val mockProject = project as? MockProject ?: return
        mockProject.registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManagerForPlatformTypeTest::class.java)
    }
}
