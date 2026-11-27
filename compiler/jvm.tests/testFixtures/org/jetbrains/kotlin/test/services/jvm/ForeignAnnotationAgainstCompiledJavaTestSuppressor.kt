/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.jvm

import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.SOURCE_RETENTION_ANNOTATIONS
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.baseTestDataFile
import org.jetbrains.kotlin.test.utils.isDirectiveDefined

class ForeignAnnotationAgainstCompiledJavaTestSuppressor(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        // The test data rather than the variant the test reads, so that a variant diverging from it is still checked
        return testServices.baseTestDataFile.isDirectiveDefined("// ${SOURCE_RETENTION_ANNOTATIONS.name}")
    }
}
