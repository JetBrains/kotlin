/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.ClassTypePointerFactory
import com.intellij.psi.impl.smartPointers.PsiClassReferenceTypePointerFactory
import org.jetbrains.kotlin.analysis.api.symbols.AdditionalKDocResolutionProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInsVirtualFileProviderCliImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.DummyFileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.providers.impl.KotlinFakeClsStubsCache
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentConfigurationBase
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentConfigurator

/**
 * [StandaloneApplicationEnvironmentConfiguration] is the base application configuration for the Analysis API. A subclass of it is expected
 * by [StandaloneProjectFactory]. It is used both for the standalone mode and Analysis API tests (including non-standalone tests).
 */
open class StandaloneApplicationEnvironmentConfiguration(
    override val isUnitTestMode: Boolean,
) : KotlinCoreApplicationEnvironmentConfigurationBase() {
    init {
        addConfigurator(StandaloneApplicationEnvironmentConfigurator)
    }
}

object DefaultStandaloneApplicationEnvironmentConfigurations {
    val PRODUCTION = StandaloneApplicationEnvironmentConfiguration(isUnitTestMode = false)

    /**
     * This test application environment configuration is NOT used for Analysis API tests, but rather when a third party creates a
     * standalone Analysis API session in unit test mode.
     *
     * The base application environment configuration for Analysis API tests is `AnalysisApiBaseTestApplicationEnvironmentConfiguration`.
     */
    val TEST = StandaloneApplicationEnvironmentConfiguration(isUnitTestMode = true)

    fun getByUnitTestMode(isUnitTestMode: Boolean): StandaloneApplicationEnvironmentConfiguration = if (isUnitTestMode) TEST else PRODUCTION
}

private object StandaloneApplicationEnvironmentConfigurator : KotlinCoreApplicationEnvironmentConfigurator {
    override fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        registerApplicationExtensionPoints(applicationEnvironment)
        registerApplicationServices(applicationEnvironment)
    }

    private fun registerApplicationExtensionPoints(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        val applicationArea = applicationEnvironment.application.extensionArea
        val applicationDisposable = applicationEnvironment.parentDisposable

        CoreApplicationEnvironment.registerApplicationExtensionPoint(
            AdditionalKDocResolutionProvider.EP_NAME,
            AdditionalKDocResolutionProvider::class.java,
        )

        CoreApplicationEnvironment.registerApplicationExtensionPoint(
            ClassTypePointerFactory.EP_NAME,
            ClassTypePointerFactory::class.java
        )

        applicationArea
            .getExtensionPoint(ClassTypePointerFactory.EP_NAME)
            .registerExtension(PsiClassReferenceTypePointerFactory(), applicationDisposable)
    }

    private fun registerApplicationServices(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        applicationEnvironment.application.apply {
            registerService(KotlinFakeClsStubsCache::class.java, KotlinFakeClsStubsCache::class.java)
            registerService(ClsKotlinBinaryClassCache::class.java)
            registerService(
                BuiltInsVirtualFileProvider::class.java,
                BuiltInsVirtualFileProviderCliImpl(applicationEnvironment.jarFileSystem as CoreJarFileSystem)
            )
            registerService(FileAttributeService::class.java, DummyFileAttributeService::class.java)
        }
    }
}
