/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.*

/**
 * Provides implementations from the FIR Standalone module to the API one.
 */
@KaImplementationDetail
public interface KaStandaloneInternalsProvider {
    @KaImplementationDetail
    public companion object {
        private const val IMPL = "org.jetbrains.kotlin.analysis.api.standalone.base.KaStandaloneInternalsProviderImpl"

        @JvmStatic
        public val instance: KaStandaloneInternalsProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                val implClass = Class.forName(IMPL)
                implClass.getDeclaredField("INSTANCE").get(null) as KaStandaloneInternalsProvider
            } catch (e: Exception) {
                throw IllegalStateException("KaStandaloneInternalsProvider implementation not found: $IMPL", e)
            }
        }
    }

    public fun getStandaloneSessionBuilder(projectDisposable: Disposable, unitTestMode: Boolean): StandaloneAnalysisAPISessionBuilder

    public fun getSourceModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
    ): KtSourceModuleBuilder

    public fun getLibraryModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
        isSdk: Boolean
    ): KtLibraryModuleBuilder

    public fun getSdkModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
    ): KtSdkModuleBuilder

    public fun getLibrarySourceModuleBuilder(
        project: Project,
    ): KtLibrarySourceModuleBuilder

    @OptIn(KaExperimentalApi::class)
    public fun getScriptModuleBuilder(
        project: Project,
    ): KtScriptModuleBuilder
}

