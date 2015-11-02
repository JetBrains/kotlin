/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsElementImpl
import junit.framework.TestCase
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File
import java.util.regex.Pattern

object LightClassTestCommon {
    private val SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE)

    @JvmOverloads
    fun testLightClass(
            testDataFile: File,
            findLightClass: (String) -> PsiClass?,
            normalizeText: (String) -> String = { it }
    ) {
        val text = FileUtil.loadFile(testDataFile, true)
        val matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text)
        TestCase.assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find())
        val fqName = matcher.group(1)

        val lightClass = findLightClass(fqName)

        val actual = actualText(fqName, lightClass, normalizeText)
        JetTestUtils.assertEqualsToFile(JetTestUtils.replaceExtension(testDataFile, "java"), actual)
    }

    private fun actualText(fqName: String?, lightClass: PsiClass?, normalizeText: (String) -> String): String {
        if (lightClass == null) {
            return "<not generated>"
        }
        TestCase.assertTrue("Not a light class: $lightClass ($fqName)", lightClass is KtLightClass)

        val delegate = (lightClass as KtLightClass).getDelegate()
        TestCase.assertTrue("Not a CLS element: $delegate", delegate is ClsElementImpl)

        val buffer = StringBuilder()
        (delegate as ClsElementImpl).appendMirrorText(0, buffer)
        val actual = normalizeText(buffer.toString())
        return actual
    }
}