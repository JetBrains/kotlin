/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.DuplicatedFirSourceElementsException
import java.io.File

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

fun addExternalTestFiles(testDataFilePath: String) {
    File(testDataFilePath).getExternalFiles().forEach(::addFile)
}

private fun addFile(file: File) {
    addFile(FileUtil.loadFile(file, /* convertLineSeparators = */true), file.name)
}

private fun addFile(text: String, fileName: String) {
    runWriteAction {
        val virtualDir = LightPlatformTestCase.getSourceRoot()!!
        val virtualFile = virtualDir.createChildData(null, fileName)
        virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
    }
}

private fun File.getExternalFiles(): List<File> {
    val directory = parentFile
    val externalFileName = "${nameWithoutExtension}.external"
    return directory.listFiles { _, name ->
        name == "$externalFileName.kt" || name == "$externalFileName.java"
    }!!.filterNotNull()
}

/**
 * Temporary
 * @see org.jetbrains.kotlin.idea.fir.low.level.api.DuplicatedFirSourceElementsException.IS_ENABLED
 */
inline fun <T> withPossiblyDisabledDuplicatedFirSourceElementsException(fileText: String, action: () -> T): T {
    val isDisabled = InTextDirectivesUtils.isDirectiveDefined(fileText, "IGNORE_DUPLICATED_FIR_SOURCE_EXCEPTION")

    @Suppress("LiftReturnOrAssignment")
    if (isDisabled) {
        val wasEnabled = DuplicatedFirSourceElementsException.IS_ENABLED
        DuplicatedFirSourceElementsException.IS_ENABLED = false
        try {
            return action()
        } finally {
            DuplicatedFirSourceElementsException.IS_ENABLED = wasEnabled
        }
    } else {
        return action()
    }
}