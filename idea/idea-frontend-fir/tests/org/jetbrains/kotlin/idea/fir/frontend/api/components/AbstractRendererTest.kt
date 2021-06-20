/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.components

import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.test.framework.AbstractKtIdeaTestWithSingleTestFileTest
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractRendererTest : AbstractKtIdeaTestWithSingleTestFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val options = KtDeclarationRendererOptions.DEFAULT.copy(
            approximateTypes = true,
            renderContainingDeclarations = true,
            typeRendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )

        val actual = executeOnPooledThreadInReadAction {
            buildString {
                ktFile.declarations.forEach {
                    analyse(it) {
                        append(it.getSymbol().render(options))
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToFile(testDataFileSibling(".rendered"), actual)
    }
}
