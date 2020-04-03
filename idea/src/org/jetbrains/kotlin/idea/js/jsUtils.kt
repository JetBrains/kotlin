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

package org.jetbrains.kotlin.idea.js

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.isGradleModule
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

val Module.jsTestOutputFilePath: String?
    get() {
        KotlinFacet.get(this)?.configuration?.settings?.testOutputPath?.let { return it }

        if (!shouldUseJpsOutput) return null

        val compilerExtension = CompilerModuleExtension.getInstance(this)
        val outputDir = compilerExtension?.compilerOutputUrlForTests ?: return null
        return JpsPathUtil.urlToPath("$outputDir/${name}_test.js")
    }

val Module.jsProductionOutputFilePath: String?
    get() {
        KotlinFacet.get(this)?.configuration?.settings?.productionOutputPath?.let { return it }

        if (!shouldUseJpsOutput) return null

        val compilerExtension = CompilerModuleExtension.getInstance(this)
        val outputDir = compilerExtension?.compilerOutputUrl ?: return null
        return JpsPathUtil.urlToPath("$outputDir/$name.js")
    }

fun Module.asJsModule(): Module? = takeIf { it.platform.isJs() }

val Module.shouldUseJpsOutput: Boolean
    get() = !(isGradleModule() && GradleProjectSettings.isDelegatedBuildEnabled(this))