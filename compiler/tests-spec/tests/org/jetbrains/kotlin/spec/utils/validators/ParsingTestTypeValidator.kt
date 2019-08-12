/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.validators

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.spec.utils.TestType
import org.jetbrains.kotlin.spec.utils.models.AbstractSpecTest
import java.io.File

class ParsingTestTypeValidator(
    private val psiFile: PsiFile,
    testDataFile: File,
    testInfo: AbstractSpecTest
) : AbstractTestValidator(testInfo, testDataFile) {
    private fun checkErrorElement(psi: PsiElement): Boolean =
        psi.children.any { it is PsiErrorElement || checkErrorElement(it) }

    override fun computeTestTypes() = mapOf(1 to if (checkErrorElement(psiFile)) TestType.NEGATIVE else TestType.POSITIVE)
}
