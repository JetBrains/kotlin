/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

object LightClassTestCommon {
    private val SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE)

    @JvmOverloads
    fun testLightClass(
            expectedFile: File,
            testDataFile: File,
            findLightClass: (String) -> PsiClass?,
            normalizeText: (String) -> String
    ) {
        val text = FileUtil.loadFile(testDataFile, true)
        val matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text)
        TestCase.assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find())
        val fqName = matcher.group(1)

        val lightClass = findLightClass(fqName)

        val actual = actualText(fqName, lightClass, normalizeText)
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
    }

    private fun actualText(fqName: String?, lightClass: PsiClass?, normalizeText: (String) -> String): String {
        if (lightClass == null) {
            return "<not generated>"
        }
        TestCase.assertTrue("Not a light class: $lightClass ($fqName)", lightClass is KtLightClass)

        val delegate = (lightClass as KtLightClass).clsDelegate
        TestCase.assertTrue("Not a CLS element: $delegate", delegate is ClsElementImpl)

        val buffer = StringBuilder()
        (delegate as ClsElementImpl).appendMirrorText(0, buffer)

        return normalizeText(buffer.toString())
    }

    // Actual text for light class is generated with ClsElementImpl.appendMirrorText() that can find empty DefaultImpl inner class in stubs
    // for all interfaces. This inner class can't be used in Java as it generally is not seen from light classes built from Kotlin sources.
    // It is also omitted during classes generation in backend so it also absent in light classes built from compiled code.
    fun removeEmptyDefaultImpls(text: String) : String = text.replace("\n    static final class DefaultImpls {\n    }\n", "")
}