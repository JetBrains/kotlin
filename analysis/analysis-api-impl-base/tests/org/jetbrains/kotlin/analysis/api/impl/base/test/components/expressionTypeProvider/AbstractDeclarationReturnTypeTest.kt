/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider

import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.getNameWithPositionString
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDeclarationReturnTypeTest(
    configurator: FrontendApiTestConfiguratorService
) : AbstractHLApiSingleFileTest(configurator) {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val actual = buildString {
            ktFile.accept(object : KtTreeVisitor<Int>() {
                override fun visitDeclaration(dcl: KtDeclaration, data: Int): Void? {
                    if (dcl is KtTypeParameter) return null
                    append(" ".repeat(data))
                    if (dcl is KtClassLikeDeclaration) {
                        appendLine(dcl.getNameWithPositionString())
                    } else {
                        analyseForTest(dcl) {
                            val returnType = dcl.getReturnKtType()
                            append(dcl.getNameWithPositionString())
                            append(" : ")
                            appendLine(returnType.render())
                        }
                    }
                    return super.visitDeclaration(dcl, data + 2)
                }
            }, 0)
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}