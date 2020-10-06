/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsElementImpl
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.PsiClassRenderer.renderClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

object LightClassTestCommon {
    private val SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE)
    private const val NOT_GENERATED_DIRECTIVE = "// NOT_GENERATED"

    fun testLightClass(
        expectedFile: File,
        testDataFile: File,
        findLightClass: (String) -> PsiClass?,
        normalizeText: (String) -> String,
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
            return NOT_GENERATED_DIRECTIVE
        }
        TestCase.assertTrue("Not a light class: $lightClass ($fqName)", lightClass is KtLightClass)
        return normalizeText(lightClass.renderClass(renderInner = true))
    }

    // Actual text for light class is generated with ClsElementImpl.appendMirrorText() that can find empty DefaultImpl inner class in stubs
    // for all interfaces. This inner class can't be used in Java as it generally is not seen from light classes built from Kotlin sources.
    // It is also omitted during classes generation in backend so it also absent in light classes built from compiled code.
    fun removeEmptyDefaultImpls(text: String): String = text.replace("\n    static final class DefaultImpls {\n    }\n", "")
}