/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

@Suppress("UnstableApiUsage")
abstract class AbstractKotlinLambdasHintsProvider :
    InlayHintsProviderTestCase() { // Abstract- prefix is just a convention for GenerateTests

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(testPath: String) { // named according to the convention imposed by GenerateTests
        assertThatActualHintsMatch(testPath)
    }

    private fun assertThatActualHintsMatch(fileName: String) {
        with(KotlinLambdasHintsProvider()) {
            val fileContents = FileUtil.loadFile(File(fileName), true)
            val settings = createSettings()
            with(settings) {
                when (InTextDirectivesUtils.findStringWithPrefixes(fileContents, "// MODE: ")) {
                    "return" -> set(returns = true)
                    "receivers_params" -> set(receiversAndParams = true)
                    "return-&-receivers_params" -> set(returns = true, receiversAndParams = true)
                    else -> set()
                }
            }

            testProvider("KotlinLambdasHintsProvider.kt", fileContents, this, settings)
        }
    }

    private fun KotlinLambdasHintsProvider.Settings.set(returns: Boolean = false, receiversAndParams: Boolean = false) {
        this.returnExpressions = returns
        this.implicitReceiversAndParams = receiversAndParams
    }
}