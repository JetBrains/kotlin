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

package org.jetbrains.kotlin.idea.test

/*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.VfsTestUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.test.KotlinMultiModuleJava9ProjectDescriptor.ModuleDescriptor


abstract class KotlinLightJava9ModulesCodeInsightFixtureTestCase : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinMultiModuleJava9ProjectDescriptor

    override fun tearDown() {
        KotlinMultiModuleJava9ProjectDescriptor.cleanupSourceRoots()
        super.tearDown()
    }

    protected fun addFile(path: String, text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN): VirtualFile =
            VfsTestUtil.createFile(module.root(), path, text)

    protected fun addKotlinFile(path: String, @Language("kotlin") text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN): VirtualFile =
            addFile(path, text.toTestData(), module)

    protected fun addJavaFile(path: String, @Language("java") text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN): VirtualFile =
            addFile(path, text.toTestData(), module)

    protected fun moduleInfo(@Language("JAVA") text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN) =
            addFile("module-info.java", text.toTestData(), module)

    protected fun checkModuleInfo(@Language("JAVA") text: String) =
            myFixture.checkResult("module-info.java", text.toTestData(), false)
}


private const val IDENTIFIER_CARET = "CARET"
private const val COMMENT_CARET_CHAR = "/*|*/"
private const val COMMENT_CARET = "/*CARET*/"
private val ADDITIONAL_CARET_MARKERS = arrayOf(IDENTIFIER_CARET, COMMENT_CARET_CHAR, COMMENT_CARET)

private fun String.toTestData(): String =
        ADDITIONAL_CARET_MARKERS.fold(trimIndent()) { result, marker -> result.replace(marker, EditorTestUtil.CARET_TAG, ignoreCase = true) }
*/