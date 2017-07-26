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

package org.jetbrains.kotlin.gradle.kdsl

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectOpenProcessor
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleProjectOpenProcessor(builder: GradleProjectImportBuilder) : GradleProjectOpenProcessor(builder) {
    override fun canOpenProject(file: VirtualFile): Boolean = when {
        !file.isDirectory -> file.name.endsWith(".gradle.kts")
        supportedExtensions.any { file.findChild(it) != null } -> true
        else -> super.canOpenProject(file)
    }

    override fun getSupportedExtensions(): Array<String> =
            arrayOf("${GradleConstants.DEFAULT_SCRIPT_NAME}.kts", GradleConstants.SETTINGS_FILE_NAME)
}