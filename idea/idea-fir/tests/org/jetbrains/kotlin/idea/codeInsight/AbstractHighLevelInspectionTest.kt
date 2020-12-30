/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.testFramework.TestLoggerFactory

abstract class AbstractHighLevelInspectionTest : AbstractInspectionTest() {

    override fun setUp() {
        try {
            superSetUp()
            EntryPointsManagerBase.getInstance(project).ADDITIONAL_ANNOTATIONS.add(ENTRY_POINT_ANNOTATION)
//            This method is copy of super.setUp but for now I don't care about groovy and there is ClassNotFoundError and
//                        by comment this line I workaround this error.
//            runWriteAction { FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle") }
        } catch (e: Throwable) {
            TestLoggerFactory.onTestFinished(false)
            throw e
        }
    }
}