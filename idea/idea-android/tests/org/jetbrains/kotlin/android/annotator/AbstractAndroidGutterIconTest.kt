/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.annotator

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.ui.ColorIcon
import junit.framework.TestCase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.awt.Color
import java.io.File
import javax.swing.ImageIcon


abstract class AbstractAndroidGutterIconTest : KotlinAndroidTestCase() {

    fun doTest(path: String) {
        val testFile = File(path)
        val testFileText = FileUtil.loadFile(testFile)
        val withRuntime = InTextDirectivesUtils.isDirectiveDefined(testFileText, "// WITH_RUNTIME")

        val drawable = InTextDirectivesUtils.isDirectiveDefined(testFileText, "// DRAWABLE")
        val color = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// COLOR: ").takeIf { it.isNotEmpty() }?.let {
            val components = it.map { it.toInt(16) }
            Color(components[0], components[1], components[2])
        }

        try {
            if (withRuntime) {
                ConfigLibraryUtil.configureKotlinRuntime(myFixture.module)
            }

            copyResourceDirectoryForTest(path)
            myFixture.copyFileToProject(testFile.parent + "/R.java", "gen/${COM_MYAPP_PACKAGE_PATH}R.java")

            val sourceFile = myFixture.copyFileToProject(path, "src/${PathUtil.getFileName(path)}")
            myFixture.configureFromExistingVirtualFile(sourceFile)

            val gutter = myFixture.findGuttersAtCaret().find {
                when {
                    drawable -> it.icon is ImageIcon
                    color != null -> (it.icon as? ColorIcon)?.iconColor == color
                    else -> true
                }
            }

            TestCase.assertNotNull(gutter)
        } finally {
            if (withRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(myFixture.module)
            }
        }
    }

    companion object {
        private val COM_MYAPP_PACKAGE_PATH: String = "com/myapp/"
    }
}