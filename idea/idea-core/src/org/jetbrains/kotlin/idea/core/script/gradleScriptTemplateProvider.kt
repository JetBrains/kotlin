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

package org.jetbrains.kotlin.idea.core.script

import org.jetbrains.kotlin.script.ScriptTemplateProvider
import java.io.File

class GradleScriptTemplateProvider: ScriptTemplateProvider {

    private val gradleHome = System.getProperty("idea.gradle.home")?.let { File(it) }?.let { if (it.exists()) it else null }

    override val id: String = "Gradle"
    override val version: Int = 1
    override val isValid: Boolean = gradleHome != null

    override val templateClass: String = "org.gradle.script.lang.kotlin.KotlinBuildScript"
    override val dependenciesClasspath: Iterable<String> = listOf(File(gradleHome, "gradle_all.jar").absolutePath)
    override val context: Any? = gradleHome
}
