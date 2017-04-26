/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

            val gutter = myFixture.findAllGutters().find {
                when {
                    drawable -> it.icon is ImageIcon
                    color != null -> (it.icon as? ColorIcon)?.iconColor == color
                    else -> error("COLOR or DRAWABLE must be defined!")
                }
            }

            TestCase.assertNotNull(gutter)
        }
        finally {
            if (withRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(myFixture.module)
            }
        }
    }

    companion object {
        private val COM_MYAPP_PACKAGE_PATH: String = "com/myapp/"
    }
}