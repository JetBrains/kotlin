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

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleProjectImportProvider(builder: GradleProjectImportBuilder)
    : AbstractExternalProjectImportProvider(builder, GradleConstants.SYSTEM_ID) {

    override fun canImportFromFile(file: VirtualFile): Boolean = file.name.endsWith(".${GradleConstants.EXTENSION}.kts")

    override fun getFileSample(): String? = "<b>Gradle (Kotlin DSL)</b> build script (*.gradle.kts)"
}
