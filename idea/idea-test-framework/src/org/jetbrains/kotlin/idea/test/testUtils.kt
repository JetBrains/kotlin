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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.Consumer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.decompiler.JetClassFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.JetClsFile
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver
import org.jetbrains.kotlin.psi.JetFile
import java.util.*

public enum class ModuleKind {
    KOTLIN_JVM_WITH_STDLIB_SOURCES,
    KOTLIN_JAVASCRIPT
}

public fun Module.configureAs(descriptor: JetLightProjectDescriptor) {
    val module = this
    updateModel(module, object : Consumer<ModifiableRootModel> {
        override fun consume(model: ModifiableRootModel) {
            if (descriptor.getSdk() != null) {
                model.setSdk(descriptor.getSdk())
            }
            val entries = model.getContentEntries()
            if (entries.isEmpty()) {
                descriptor.configureModule(module, model)
            }
            else {
                descriptor.configureModule(module, model, entries[0])
            }
        }
    })
}

public fun Module.configureAs(kind: ModuleKind) {
    when(kind) {
        ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES ->
            this.configureAs(ProjectDescriptorWithStdlibSources.INSTANCE)
        ModuleKind.KOTLIN_JAVASCRIPT -> {
            this.configureAs(KotlinStdJSProjectDescriptor.instance)
            KotlinJavaScriptLibraryManager.getInstance(this.getProject()).syncUpdateProjectLibrary()
        }

        else -> throw IllegalArgumentException("Unknown kind=$kind")
    }
}

public fun JetFile.dumpTextWithErrors(): String {
    val diagnostics = analyzeFullyAndGetResult().bindingContext.getDiagnostics()
    val errors = diagnostics.filter { it.getSeverity() == Severity.ERROR }
    if (errors.isEmpty()) return getText()
    val header = errors.map { "// ERROR: " + DefaultErrorMessages.render(it).replace('\n', ' ') }.joinToString("\n", postfix = "\n")
    return header + getText()
}

public fun closeAndDeleteProject(): Unit =
    ApplicationManager.getApplication().runWriteAction() { LightPlatformTestCase.closeAndDeleteProject() }

public fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: RunnableWithException) {
    val builtInsSources = BuiltInsReferenceResolver.getInstance(project).builtInsSources!!

    val stdLibViewProviders = HashSet<JetClassFileViewProvider>()
    val vFileToViewProviderMap = ((PsiManager.getInstance(project) as PsiManagerEx).fileManager as FileManagerImpl).vFileToViewProviderMap
    for ((file, viewProvider) in vFileToViewProviderMap) {
        if (file.isStdLibFile && viewProvider is JetClassFileViewProvider) {
            stdLibViewProviders.add(viewProvider)
        }
    }

    runnable.run()

    // Base tearDown() invalidates builtins and std-lib files. Restore them with brute force.
    fun unInvalidateFile(file: PsiFileImpl) {
        val field = javaClass<PsiFileImpl>().getDeclaredField("myInvalidated")!!
        field.setAccessible(true)
        field.set(file, false)
    }

    builtInsSources.forEach { unInvalidateFile(it) }
    stdLibViewProviders.forEach {
        it.allFiles.forEach { unInvalidateFile(it as JetClsFile) }
        vFileToViewProviderMap.set(it.virtualFile, it)
    }
}

private val VirtualFile.isStdLibFile: Boolean get() = presentableUrl.contains("kotlin-runtime.jar")

public fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: () -> Unit) {
    unInvalidateBuiltinsAndStdLib(project, RunnableWithException { runnable() })
}

public fun invalidateLibraryCache(project: Project) {
    LibraryModificationTracker.getInstance(project).incModificationCount()
}