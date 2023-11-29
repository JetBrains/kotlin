/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneApplicationEnvironmentConfiguration
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentConfigurator

/**
 * This application environment configuration is the base configuration for most Analysis API tests. Custom application environment
 * configurations for specific Analysis API tests should usually inherit from this class.
 */
abstract class AnalysisApiBaseTestApplicationEnvironmentConfiguration :
    StandaloneApplicationEnvironmentConfiguration(isUnitTestMode = true) {
    init {
        addConfigurator(AnalysisApiBaseTestApplicationEnvironmentConfigurator)
    }
}

/**
 * This test configuration is used for Analysis API tests which don't require any additional configuration of the application environment,
 * which covers most non-library Analysis API tests.
 */
object DefaultAnalysisApiTestApplicationEnvironmentConfiguration : AnalysisApiBaseTestApplicationEnvironmentConfiguration()

private object AnalysisApiBaseTestApplicationEnvironmentConfigurator : KotlinCoreApplicationEnvironmentConfigurator {
    override fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        applicationEnvironment.registerFileType(KotlinBuiltInFileType, "kotlin_builtins")
        applicationEnvironment.registerFileType(KotlinBuiltInFileType, "kotlin_metadata")
        applicationEnvironment.registerFileType(KlibMetaFileType, "knm")
    }
}
