/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KotlinOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtElement
import java.io.File

@OptIn(InvalidWayOfUsingAnalysisSession::class)
fun Project.invalidateCaches(context: KtElement?) {
    LibraryModificationTracker.getInstance(this).incModificationCount()
    service<KotlinOutOfBlockModificationTrackerFactory>().incrementModificationsCount()
    service<KtAnalysisSessionProvider>().clearCaches()
    if (context != null) {
        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction { analyse(context) { } }
        }.get()
    }
}

fun addExternalTestFiles(testDataFilePath: String) {
    File(testDataFilePath).getExternalFiles().forEach(::addFile)
}

private fun addFile(file: File) {
    addFile(FileUtil.loadFile(file, /* convertLineSeparators = */true), file.name)
}

private fun File.getExternalFiles(): List<File> {
    val directory = parentFile
    val externalFileName = "${nameWithoutExtension}.external"
    return directory.listFiles { _, name ->
        name == "$externalFileName.kt" || name == "$externalFileName.java"
    }!!.filterNotNull()
}


private fun addFile(text: String, fileName: String) {
    runWriteAction {
        val virtualDir = LightPlatformTestCase.getSourceRoot()!!
        val virtualFile = virtualDir.createChildData(null, fileName)
        virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
    }
}