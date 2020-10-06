/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Paths


abstract class AbstractLightTree2FirConverterTestCase : AbstractRawFirBuilderTestCase() {

    override fun doTest(filePath: String) {
        val firFile = LightTree2Fir(
            session = FirSessionFactory.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            stubMode = false
        ).buildFirFile(Paths.get(filePath))
        val firDump = firFile.render()

        val expectedFile = File(filePath.replace(".kt", ".txt"))
        KotlinTestUtils.assertEqualsToFile(expectedFile, firDump)
    }
}
