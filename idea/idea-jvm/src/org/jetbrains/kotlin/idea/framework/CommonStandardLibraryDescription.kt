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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryKind

class CommonStandardLibraryDescription(project: Project?) : CustomLibraryDescriptorWithDeferredConfig(
        // TODO: KotlinCommonModuleConfigurator
        project, "common", LIBRARY_NAME, DIALOG_TITLE, LIBRARY_CAPTION, KOTLIN_COMMON_STDLIB_KIND, SUITABLE_LIBRARY_KINDS
) {
    companion object {
        val KOTLIN_COMMON_STDLIB_KIND = LibraryKind.create("kotlin-stdlib-common")
        val LIBRARY_NAME = "KotlinStdlibCommon"

        val DIALOG_TITLE = "Create Kotlin Common Standard Library"
        val LIBRARY_CAPTION = "Kotlin Common Standard Library"
        val SUITABLE_LIBRARY_KINDS: Set<LibraryKind> = setOf(KOTLIN_COMMON_STDLIB_KIND)
    }
}
