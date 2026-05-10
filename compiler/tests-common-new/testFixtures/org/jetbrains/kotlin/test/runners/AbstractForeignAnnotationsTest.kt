/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.codegen.forTestCompile.JavaForeignAnnotationType
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.SKIP_TXT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ANNOTATIONS_PATH
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.PROVIDE_JAVA_AS_BINARIES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_JSR305_TEST_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_THIRD_PARTY_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.preprocessors.ExternalAnnotationsSourcePreprocessor
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ExternalAnnotationsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.jvm.ForeignAnnotationAgainstCompiledJavaTestSuppressor
import org.jetbrains.kotlin.test.services.jvm.PsiClassFilesReadingForCompiledJavaTestSuppressor
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

enum class ForeignAnnotationsTestKind(val compiledJava: Boolean, val psiClassLoading: Boolean) {
    SOURCE(false, false),
    COMPILED_JAVA(true, false),
    COMPILED_JAVA_WITH_PSI_CLASS_LOADING(true, true)
}

abstract class AbstractForeignAnnotationsTestBase(
    private val kind: ForeignAnnotationsTestKind,
    private val parser: FirParser,
) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            if (kind.compiledJava) {
                +PROVIDE_JAVA_AS_BINARIES
                +SKIP_TXT
            }
            if (kind.psiClassLoading) {
                +USE_PSI_CLASS_FILES_READING
            }
            +ENABLE_FOREIGN_ANNOTATIONS
            +WITH_THIRD_PARTY_ANNOTATIONS
        }

        configureFirParser(parser)

        enableMetaInfoHandler()

        if (kind.compiledJava) {
            useMetaTestConfigurators(::ForeignAnnotationAgainstCompiledJavaTestSuppressor)
        }
        if (kind.psiClassLoading) {
            useMetaTestConfigurators(::PsiClassFilesReadingForCompiledJavaTestSuppressor)
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmForeignAnnotationsConfigurator,
            ::JvmEnvironmentConfigurator,
            ::ExternalAnnotationsEnvironmentConfigurator,
        )

        useSourcePreprocessor(::ExternalAnnotationsSourcePreprocessor)

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(::FirFrontendFacade)
        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
                ::FirDistinctSourceElementsHandler,
            )
        }

        useFailureSuppressors(::FirFailingTestSuppressor)

        useMetaInfoProcessors(::PsiLightTreeMetaInfoProcessor)

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Annotations
                +WITH_JSR305_TEST_ANNOTATIONS
            }
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/tests/jakarta/*") {
            defaultDirectives {
                JDK_KIND with TestJdkKind.FULL_JDK_11
            }
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java8Tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Java8Annotations
            }
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java11Tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Java9Annotations
                JDK_KIND with TestJdkKind.FULL_JDK_11
            }
        }
    }
}

abstract class AbstractFirPsiForeignAnnotationsSourceJavaTest :
    AbstractForeignAnnotationsTestBase(ForeignAnnotationsTestKind.SOURCE, FirParser.Psi) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            firHandlersStep {
                // Can only be applied to either SOURCE of COMPILED as parameter names are not preserved when compiled.
                useHandlers(::FirScopeDumpHandler)
            }
        }
    }
}

abstract class AbstractFirPsiForeignAnnotationsCompiledJavaTest :
    AbstractForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA, FirParser.Psi)

abstract class AbstractFirPsiForeignAnnotationsCompiledJavaWithPsiClassReadingTest :
    AbstractForeignAnnotationsTestBase(ForeignAnnotationsTestKind.COMPILED_JAVA_WITH_PSI_CLASS_LOADING, FirParser.Psi)
