/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder.KtLibraryModuleBuilderImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder.KtLibrarySourceModuleBuilderImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder.KtScriptModuleBuilderImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder.KtSourceModuleBuilderImpl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtLibraryModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtLibrarySourceModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtScriptModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtSdkModuleBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.KtSourceModuleBuilder

@KaImplementationDetail
internal object KaStandaloneInternalsProviderImpl : KaStandaloneInternalsProvider {
    override fun getStandaloneSessionBuilder(
        projectDisposable: Disposable,
        unitTestMode: Boolean,
    ): StandaloneAnalysisAPISessionBuilder {
        return StandaloneAnalysisAPISessionBuilderImpl(projectDisposable, unitTestMode)
    }

    override fun getSourceModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
    ): KtSourceModuleBuilder {
        return KtSourceModuleBuilderImpl(coreApplicationEnvironment, project)
    }

    override fun getLibraryModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
        isSdk: Boolean,
    ): KtLibraryModuleBuilder {
        return KtLibraryModuleBuilderImpl(coreApplicationEnvironment, project, isSdk)
    }

    override fun getSdkModuleBuilder(
        coreApplicationEnvironment: CoreApplicationEnvironment,
        project: Project,
    ): KtSdkModuleBuilder {
        return KtLibraryModuleBuilderImpl(coreApplicationEnvironment, project, isSdk = true) as KtSdkModuleBuilder
    }

    override fun getLibrarySourceModuleBuilder(project: Project): KtLibrarySourceModuleBuilder {
        return KtLibrarySourceModuleBuilderImpl(project)
    }

    override fun getScriptModuleBuilder(project: Project): KtScriptModuleBuilder {
        return KtScriptModuleBuilderImpl(project)
    }
}
