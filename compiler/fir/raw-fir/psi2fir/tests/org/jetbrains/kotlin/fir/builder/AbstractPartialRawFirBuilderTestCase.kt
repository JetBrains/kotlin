/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPartialRawFirBuilderTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        val nameToFind = File(filePath).useLines {
            it.first().run {
                assert(startsWith(Companion.prefix))
                drop(Companion.prefix.length)
            }
        }
        val file = createKtFile(filePath)
        val functionToBuild = file.findDescendantOfType<KtNamedFunction> { it.name == nameToFind }!!

        val session = FirSessionFactory.createEmptySession()

        val firFunction = RawFirBuilder(session, StubFirScopeProvider, false)
            .buildFunctionWithBody(functionToBuild)

        val firDump = firFunction.render(FirRenderer.RenderMode.WithFqNames)
        val expectedPath = filePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firDump)
    }

    companion object {
        private const val prefix = "// FUNCTION: "
    }
}
