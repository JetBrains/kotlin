/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryType
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription
import org.jetbrains.kotlin.idea.framework.JSLibraryType
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.idea.versions.isKotlinJsRuntime
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform

open class KotlinJsModuleConfigurator : KotlinWithLibraryConfigurator() {
    override val name: String
        get() = NAME

    override val targetPlatform: TargetPlatform
        get() = JsPlatform

    override val presentableText: String
        get() = JavaScript.FULL_NAME

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
            listOf(LibraryJarDescriptor.JS_STDLIB_JAR,
                  LibraryJarDescriptor.JS_STDLIB_SRC_JAR)

    override val libraryMatcher: (Library) -> Boolean = ::isKotlinJsRuntime

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
            if (JsLibraryStdDetectionUtil.hasJsStdlibJar(library, ignoreKind = true) && library.kind == null) {
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
            collector.addMessage("Updated JavaScript libraries in module ${module.name}")
        }
        return brokenStdlib
    }
}
