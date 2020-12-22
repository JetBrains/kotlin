/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.SKIP_TXT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ANNOTATIONS_PATH
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SKIP_JAVA_SOURCES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_JAVAC
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JdkForeignAnnotationType
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsAgainstCompiledJavaConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.jvm.ForeignAnnotationAgainstCompiledJavaTestSuppressor

abstract class AbstractForeignAnnotationsTestBase : AbstractKotlinCompilerTest() {
    protected abstract val foreignAnnotationsConfigurator: Constructor<JvmForeignAnnotationsConfigurator>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            backend = BackendKind.NoBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
        }

        enableMetaInfoHandler()

        useConfigurators(
            ::JvmEnvironmentConfigurator,
            foreignAnnotationsConfigurator
        )

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(
            ::DeclarationsDumpHandler,
            ::ClassicDiagnosticsHandler,
        )

        forTestsMatching("compiler/testData/foreignAnnotations/tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JdkForeignAnnotationType.Annotations
            }
        }

        forTestsMatching("compiler/testData/foreignAnnotations/java8Tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JdkForeignAnnotationType.Jdk8Annotations
            }
        }
    }
}

abstract class AbstractForeignAnnotationsTest : AbstractForeignAnnotationsTestBase() {
    override val foreignAnnotationsConfigurator: Constructor<JvmForeignAnnotationsConfigurator>
        get() = ::JvmForeignAnnotationsConfigurator
}

abstract class AbstractForeignAnnotationsNoAnnotationInClasspathTest : AbstractForeignAnnotationsTestBase() {
    override val foreignAnnotationsConfigurator: Constructor<JvmForeignAnnotationsConfigurator>
        get() = ::JvmForeignAnnotationsAgainstCompiledJavaConfigurator

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +SKIP_JAVA_SOURCES
                +SKIP_TXT
            }

            useMetaTestConfigurators(::ForeignAnnotationAgainstCompiledJavaTestSuppressor)
        }
    }
}

abstract class AbstractForeignAnnotationsNoAnnotationInClasspathWithPsiClassReadingTest : AbstractForeignAnnotationsNoAnnotationInClasspathTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +USE_PSI_CLASS_FILES_READING
            }
        }
    }
}
