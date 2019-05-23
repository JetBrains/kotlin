/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.totalKotlin

import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File

@TestDataPath("/")
@RunWith(JUnit3RunnerWithInners::class)
class LightTree2FirConverterTotalTest : AbstractTotalKotlinTest("LightTree") {
    private var lightTreeConverter: LightTree2Fir? = null

    override fun generateTree(onlyBaseTree: Boolean, text: String, file: File) {
        if (onlyBaseTree) {
            val lightTree = lightTreeConverter!!.buildLightTree(text)
            DebugUtil.lightTreeToString(lightTree, false)
        } else {
            val firFile = lightTreeConverter!!.buildFirFile(text, file.name)
            StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
        }
    }

    private fun createConverter() {
        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        lightTreeConverter = LightTree2Fir(true, parserDefinition, lexer)
    }

    fun testTotalKotlinOnlyLightTree() {
        createConverter()
        totalKotlinTest(true)
    }

    fun testTotalKotlinFirFromLightTree() {
        createConverter()
        totalKotlinTest(false)
    }
}