/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryType
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription
import org.jetbrains.kotlin.idea.framework.JSLibraryType
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms

open class KotlinJsModuleConfigurator : KotlinWithLibraryConfigurator() {
    override val name: String
        get() = NAME

    override val targetPlatform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform

    @Suppress("DEPRECATION_ERROR")
    override fun getTargetPlatform() = JsPlatforms.CompatJsPlatform

    override val presentableText: String
        get() = KotlinJvmBundle.message("language.name.javascript")

    override fun isConfigured(module: Module) = hasKotlinJsRuntimeInScope(module)

    override val libraryName: String
        get() = JSLibraryStdDescription.LIBRARY_NAME

    override val dialogTitle: String
        get() = JSLibraryStdDescription.DIALOG_TITLE

    override val libraryCaption: String
        get() = JSLibraryStdDescription.LIBRARY_CAPTION

    override val messageForOverrideDialog: String
        get() = JSLibraryStdDescription.JAVA_SCRIPT_LIBRARY_CREATION

    override fun getLibraryJarDescriptors(sdk: Sdk?): List<LibraryJarDescriptor> =
        listOf(
            LibraryJarDescriptor.JS_STDLIB_JAR,
            LibraryJarDescriptor.JS_STDLIB_SRC_JAR
        )

    override val libraryMatcher: (Library, Project) -> Boolean = { library, project ->
        JsLibraryStdDetectionUtil.hasJsStdlibJar(library, project)
    }

    override val libraryType: LibraryType<DummyLibraryProperties>?
        get() = JSLibraryType.getInstance()

    companion object {
        const val NAME = JavaScript.LOWER_NAME
    }

    /**
     * Migrate pre-1.1.3 projects which didn't have explicitly specified kind for JS libraries.
     */
    override fun findAndFixBrokenKotlinLibrary(module: Module, collector: NotificationMessageCollector): Library? {
        val allLibraries = mutableListOf<LibraryEx>()
        var brokenStdlib: Library? = null
        for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
            val library = (orderEntry as? LibraryOrderEntry)?.library as? LibraryEx ?: continue
            allLibraries.add(library)
            if (JsLibraryStdDetectionUtil.hasJsStdlibJar(library, module.project, ignoreKind = true) && library.kind == null) {
                brokenStdlib = library
            }
        }

        if (brokenStdlib != null) {
            runWriteAction {
                for (library in allLibraries.filter { it.kind == null }) {
                    library.modifiableModel.apply {
                        kind = JSLibraryKind
                        commit()
                    }
                }
            }
            collector.addMessage(KotlinJvmBundle.message("updated.javascript.libraries.in.module.0", module.name))
        }
        return brokenStdlib
    }
}
