/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.totalKotlin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("/")
@RunWith(JUnit3RunnerWithInners::class)
class Psi2FirConverterTotalTest : AbstractTotalKotlinTest("Psi") {
    override fun generateTree(onlyBaseTree: Boolean, text: String, file: File) {
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
        if (onlyBaseTree) {
            DebugUtil.psiTreeToString(ktFile, false)
        } else {
            val firFile = ktFile.toFirFile(stubMode = true)
            StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
        }
    }

    fun testTotalKotlinOnlyPsi() {
        totalKotlinTest(true)
    }

    fun testTotalKotlinFirFromPsi() {
        totalKotlinTest(false)
    }
}