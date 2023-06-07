/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticModuleProvider
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.JvmFirDeserializedSymbolProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPsiDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

public object StandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        val projectStructureProvider = project.getService(ProjectStructureProvider::class.java)
        val binaryModules =
            (projectStructureProvider as? KtStaticModuleProvider)?.allModules?.filterIsInstance<KtBinaryModule>()
                ?: emptyList()
        val projectEnvironment = testServices.environmentManager.getProjectEnvironment()
        project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory(
                    this,
                    binaryModules,
                    projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
                )
            )
            registerService(JvmFirDeserializedSymbolProviderFactory::class.java, JvmFirDeserializedSymbolProviderFactory::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
    }
}