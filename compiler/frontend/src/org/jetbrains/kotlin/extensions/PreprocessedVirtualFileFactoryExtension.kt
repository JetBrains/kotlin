/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * The interface for the extensions that are used to substitute VirtualFile on the creation of KtFile, allows to preprocess a file before
 * lexing and parsing
 */
interface PreprocessedVirtualFileFactoryExtension {
    companion object : ProjectExtensionDescriptor<PreprocessedVirtualFileFactoryExtension>(
        "org.jetbrains.kotlin.preprocessedVirtualFileFactoryExtension",
        PreprocessedVirtualFileFactoryExtension::class.java
    )

    fun isPassThrough(): Boolean

    fun createPreprocessedFile(file: VirtualFile?): VirtualFile?
    fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile?
}

class PreprocessedFileCreator(val project: Project) {

    private val validExts: Array<PreprocessedVirtualFileFactoryExtension> by lazy {
        PreprocessedVirtualFileFactoryExtension.getInstances(project).filterNot { it.isPassThrough() }.toTypedArray()
    }

    fun create(file: VirtualFile): VirtualFile = validExts.firstNotNullOfOrNull { it.createPreprocessedFile(file) } ?: file

    // unused now, but could be used in the IDE at some point
    fun createLight(file: LightVirtualFile): LightVirtualFile =
        validExts.firstNotNullOfOrNull { it.createPreprocessedLightFile(file) } ?: file
}

