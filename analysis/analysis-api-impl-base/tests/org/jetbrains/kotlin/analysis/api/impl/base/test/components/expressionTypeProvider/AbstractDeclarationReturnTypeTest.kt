/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.utils.getNameWithPositionString
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDeclarationReturnTypeTest : AbstractHLApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val actual = buildString {
            ktFile.accept(object : KtTreeVisitor<Int>() {
                override fun visitDeclaration(dclaration: KtDeclaration, indent: Int): Void? {
                    if (dclaration is KtTypeParameter) return null
                    append(" ".repeat(indent))
                    if (dclaration is KtClassLikeDeclaration) {
                        appendLine(dclaration.getNameWithPositionString())
                    } else {
                        analyseForTest(dclaration) {
                            val returnType = dclaration.getReturnKtType()
                            append(dclaration.getNameWithPositionString())
                            append(" : ")
                            appendLine(returnType.render())
                        }
                    }
                    return super.visitDeclaration(dclaration, indent + 2)
                }
            }, 0)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
