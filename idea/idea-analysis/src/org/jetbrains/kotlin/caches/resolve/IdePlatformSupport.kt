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

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.kotlin.analyzer.AnalyzerFacade
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.idea.caches.resolve.JsAnalyzerFacade
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

abstract class IdePlatformSupport {
    abstract val platform: TargetPlatform
    abstract val analyzerFacade: AnalyzerFacade
    abstract val libraryKind: PersistentLibraryKind<*>?

    abstract fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns

    abstract fun isModuleForPlatform(module: Module): Boolean

    companion object {
        val EP_NAME = ExtensionPointName.create<IdePlatformSupport>("org.jetbrains.kotlin.idePlatformSupport")

        val platformSupport by lazy {
            Extensions.getExtensions(EP_NAME).associateBy { it.platform }
        }

        val facades by lazy {
            Extensions.getExtensions(EP_NAME).map { it.platform to it.analyzerFacade }.toMap()
        }

        @JvmStatic
        fun getPlatformForModule(module: Module): TargetPlatform {
            return Extensions.getExtensions(EP_NAME).find { it.isModuleForPlatform(module) }?.platform ?: JvmPlatform
        }
    }

}

class JvmPlatformSupport : IdePlatformSupport() {
    override val platform: TargetPlatform
        get() = JvmPlatform

    override val analyzerFacade: AnalyzerFacade
        get() = JvmAnalyzerFacade

    override val libraryKind: PersistentLibraryKind<*>?
        get() = null

    override fun isModuleForPlatform(module: Module): Boolean {
        val settings = KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module)
        return settings.targetPlatformKind is TargetPlatformKind.Jvm
    }

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
        return if (settings.sdk != null) JvmBuiltIns(sdkContext.storageManager) else DefaultBuiltIns.Instance
    }
}

class JsPlatformSupport : IdePlatformSupport() {
    override val platform: TargetPlatform
        get() = JsPlatform

    override val analyzerFacade: AnalyzerFacade
        get() = JsAnalyzerFacade

    override val libraryKind: PersistentLibraryKind<*>?
        get() = JSLibraryKind

    override fun isModuleForPlatform(module: Module): Boolean {
        val settings = KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module)
        return settings.targetPlatformKind is TargetPlatformKind.JavaScript
    }

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
        return JsPlatform.builtIns
    }
}

class CommonPlatformSupport : IdePlatformSupport() {
    override val platform: TargetPlatform
        get() = TargetPlatform.Default

    override val analyzerFacade: AnalyzerFacade
        get() = DefaultAnalyzerFacade

    override val libraryKind: PersistentLibraryKind<*>?
        get() = CommonLibraryKind

    override fun isModuleForPlatform(module: Module): Boolean {
        val settings = KotlinFacetSettingsProvider.getInstance(module.project).getInitializedSettings(module)
        return settings.targetPlatformKind is TargetPlatformKind.Common
    }

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }
}
