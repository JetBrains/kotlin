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

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.Consumer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import java.lang.IllegalArgumentException
import java.util.*

enum class ModuleKind {
    KOTLIN_JVM_WITH_STDLIB_SOURCES,
    KOTLIN_JAVASCRIPT
}

fun Module.configureAs(descriptor: KotlinLightProjectDescriptor) {
    val module = this
    updateModel(module, Consumer<ModifiableRootModel> { model ->
        if (descriptor.sdk != null) {
            model.sdk = descriptor.sdk
        }
        val entries = model.contentEntries
        if (entries.isEmpty()) {
            descriptor.configureModule(module, model)
        }
        else {
            descriptor.configureModule(module, model, entries[0])
        }
    })
}

fun Module.configureAs(kind: ModuleKind) {
    when(kind) {
        ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES ->
            this.configureAs(ProjectDescriptorWithStdlibSources.INSTANCE)
        ModuleKind.KOTLIN_JAVASCRIPT -> {
            this.configureAs(KotlinStdJSProjectDescriptor)
        }

    }
}

fun KtFile.dumpTextWithErrors(): String {
    val diagnostics = analyzeFully().diagnostics
    val errors = diagnostics.filter { it.severity == Severity.ERROR }
    if (errors.isEmpty()) return text
    val header = errors.map { "// ERROR: " + DefaultErrorMessages.render(it).replace('\n', ' ') }.joinToString("\n", postfix = "\n")
    return header + text
}

fun closeAndDeleteProject() = LightPlatformTestCase.closeAndDeleteProject()

fun doKotlinTearDown(project: Project, runnable: RunnableWithException) {
    doKotlinTearDown(project) { runnable.run() }
}

fun doKotlinTearDown(project: Project, runnable: () -> Unit) {
    unInvalidateBuiltinsAndStdLib(project) {
        runnable()
    }
}

fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: () -> Unit) {
    val stdLibViewProviders = HashSet<KotlinDecompiledFileViewProvider>()
    val vFileToViewProviderMap = ((PsiManager.getInstance(project) as PsiManagerEx).fileManager as FileManagerImpl).vFileToViewProviderMap
    for ((file, viewProvider) in vFileToViewProviderMap) {
        if (file.isStdLibFile && viewProvider is KotlinDecompiledFileViewProvider) {
            stdLibViewProviders.add(viewProvider)
        }
    }

    runnable()

    // Base tearDown() invalidates builtins and std-lib files. Restore them with brute force.
    fun unInvalidateFile(file: PsiFileImpl) {
        val field = PsiFileImpl::class.java.getDeclaredField("myInvalidated")!!
        field.isAccessible = true
        field.set(file, false)
    }

    stdLibViewProviders.forEach {
        it.allFiles.forEach { unInvalidateFile(it as KtDecompiledFile) }
        vFileToViewProviderMap[it.virtualFile] = it
    }
}

private val VirtualFile.isStdLibFile: Boolean get() = presentableUrl.contains("kotlin-runtime.jar")

fun invalidateLibraryCache(project: Project) {
    LibraryModificationTracker.getInstance(project).incModificationCount()
}

fun Document.extractMarkerOffset(project: Project, caretMarker: String = "<caret>"): Int {
    return extractMultipleMarkerOffsets(project, caretMarker).singleOrNull() ?: -1
}

fun Document.extractMultipleMarkerOffsets(project: Project, caretMarker: String = "<caret>"): List<Int> {
    val offsets = ArrayList<Int>()

    runWriteAction {
        val text = StringBuilder(text)
        while (true) {
            val offset = text.indexOf(caretMarker)
            if (offset >= 0) {
                text.delete(offset, offset + caretMarker.length)
                setText(text.toString())

                offsets += offset
            }
            else break
        }
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments()
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(this)

    return offsets
}