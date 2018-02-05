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
import org.jetbrains.kotlin.idea.facet.implementingModules
import org.jetbrains.kotlin.idea.framework.isGradleModule
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils

val Module.jsTestOutputFilePath: String?
    get() {
        if (!shouldUseJpsOutput) {
            (KotlinFacet.get(this)?.configuration?.settings?.testOutputPath)?.let { return it }
        }

        val compilerExtension = CompilerModuleExtension.getInstance(this)
        val outputDir = compilerExtension?.compilerOutputUrlForTests ?: return null
        return JpsPathUtil.urlToPath("$outputDir/${name}_test.js")
    }


fun Module.jsOrJsImpl() = when (TargetPlatformDetector.getPlatform(this)) {
    is TargetPlatform.Common -> implementingModules.firstOrNull { TargetPlatformDetector.getPlatform(it) is JsPlatform }
    is JsPlatform -> this
    else -> null
}

val Module.shouldUseJpsOutput: Boolean
    get() = !(isGradleModule() && GradleSystemRunningSettings.getInstance().isUseGradleAwareMake)

fun getJsOutputFilePath(module: Module, isTests: Boolean, isMeta: Boolean): String? {
    val compilerExtension = CompilerModuleExtension.getInstance(module)
    val outputDir = (if (isTests) compilerExtension?.compilerOutputUrlForTests else compilerExtension?.compilerOutputUrl)
            ?: return null
    val extension = if (isMeta) KotlinJavascriptMetadataUtils.META_JS_SUFFIX else KotlinJavascriptMetadataUtils.JS_EXT
    return JpsPathUtil.urlToPath("$outputDir/${module.name}${suffix(isTests)}$extension")
}

private fun suffix(isTests: Boolean) = if (isTests) "_test" else ""