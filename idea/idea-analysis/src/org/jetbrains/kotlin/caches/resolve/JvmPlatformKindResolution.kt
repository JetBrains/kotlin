/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade

class JvmPlatformKindResolution : IdePlatformKindResolution {
    override val kind get() = JvmIdePlatformKind

    override val resolverForModuleFactory: ResolverForModuleFactory
        get() = JvmAnalyzerFacade

    override fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
        return if (settings.sdk != null) JvmBuiltIns(sdkContext.storageManager) else DefaultBuiltIns.Instance
    }
}