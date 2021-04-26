/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.test.utils.IgnoreTests
import java.io.File

abstract class AbstractHLLocalInspectionTest : AbstractLocalInspectionTest() {
    override fun isFirPlugin() = true

    override val inspectionFileName: String = ".firInspection"

    override fun checkForUnexpectedErrors(fileText: String) {}

    override fun doTestFor(mainFile: File, inspection: AbstractKotlinInspection, fileText: String) {
        IgnoreTests.runTestIfNotDisabledByFileDirective(mainFile.toPath(), IgnoreTests.DIRECTIVES.IGNORE_FIR, "after") {
            super.doTestFor(mainFile, inspection, fileText)
        }
    }
}