/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCacheCleaner
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinForeignValueProviderService
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinActualDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.PackagePartProviderTestImpl
import org.jetbrains.kotlin.analysis.test.framework.services.TestForeignValueProviderService
import org.jetbrains.kotlin.analysis.test.framework.services.TestKotlinActualDeclarationProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KotlinPackagePartProviderFactory::class.java, PackagePartProviderTestImpl(testServices))
            registerService(KotlinActualDeclarationProvider::class.java, TestKotlinActualDeclarationProvider(project))

            // The low-memory cache cleanup only waits for `analyze` blocks, so it can invalidate sessions in the middle of a test which
            // works with FIR outside of `analyze` (KT-89816). Without the service, `KaFirCacheCleaner.getInstance` falls back to a no-op.
            // Services are keyed by the interface name, and this registrar must run after `FirStandaloneServiceRegistrar`.
            picoContainer.unregisterComponent(KaFirCacheCleaner::class.java.name)
        }
    }

    override fun registerApplicationServices(application: MockApplication, disposable: Disposable, testServices: TestServices) {
        application.apply {
            registerService(KotlinForeignValueProviderService::class.java, TestForeignValueProviderService())
        }
    }
}
