/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.diagnostic.LoadingState
import com.intellij.diagnostic.StartUpMeasurer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractRawFirBuilderLazyBodiesTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        StartUpMeasurer.setCurrentState(LoadingState.APP_STARTED)
        com.intellij.openapi.util.registry.Registry.get("ast.loading.filter").setValue(true)
        val fs = StandardFileSystems.local()
        val psiManager = PsiManager.getInstance(myProject)
        val vFile = fs.findFileByPath(filePath)!!

        val file = psiManager.findFile(vFile) as KtFile
        val firFile = file.toFirFile(BodyBuildingMode.LAZY_BODIES)
        val firFileDump = FirRenderer().renderElementAsString(firFile)
        val expectedPath = filePath.replace(".kt", ".lazyBodies.txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }
}