/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
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
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(KotlinForeignValueProviderService::class.java, TestForeignValueProviderService())
        }
    }
}
