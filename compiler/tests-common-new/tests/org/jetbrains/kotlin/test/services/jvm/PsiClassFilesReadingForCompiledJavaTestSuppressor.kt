/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.MUTE_FOR_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.isDirectiveDefined

class PsiClassFilesReadingForCompiledJavaTestSuppressor(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        return testFile.isDirectiveDefined("// ${MUTE_FOR_PSI_CLASS_FILES_READING.name}")
    }
}
