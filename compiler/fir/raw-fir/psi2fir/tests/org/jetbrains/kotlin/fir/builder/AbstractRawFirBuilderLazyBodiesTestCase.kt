/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractRawFirBuilderLazyBodiesTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        val file = createKtFile(filePath)
        val firFile = file.toFirFile(BodyBuildingMode.LAZY_BODIES)
        val firFileDump = FirRenderer().renderElementAsString(firFile)
        val expectedPath = filePath.replace(".${myFileExt}", ".lazyBodies.txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }
}