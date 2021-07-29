/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestJavacVersion
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.classicFrontendStep
import org.jetbrains.kotlin.test.builders.configureClassicFrontendHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.SKIP_TXT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ANNOTATIONS_PATH
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ALL_JAVA_AS_BINARY
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.COMPILE_JAVA_USING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_FOREIGN_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_JSR305_TEST_ANNOTATIONS
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.preprocessors.JspecifyMarksCleanupPreprocessor
import org.jetbrains.kotlin.test.services.configuration.*
import org.jetbrains.kotlin.test.services.jvm.ForeignAnnotationAgainstCompiledJavaTestSuppressor
import org.jetbrains.kotlin.test.services.jvm.PsiClassFilesReadingForCompiledJavaTestSuppressor
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

private val configureClassicFrontend: TestConfigurationBuilder.() -> Unit = {
    globalDefaults {
        frontend = FrontendKinds.ClassicFrontend
    }

    useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
    classicFrontendStep()
    classicFrontendHandlersStep {
        useHandlers(
            ::DeclarationsDumpHandler,
            ::ClassicDiagnosticsHandler,
        )
    }

    forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java8Tests/jspecify/*") {
        configureClassicFrontendHandlersStep {
            useHandlers(::JspecifyDiagnosticComplianceHandler)
        }
        useSourcePreprocessor(::JspecifyMarksCleanupPreprocessor)
    }

    useAfterAnalysisCheckers(::FirTestDataConsistencyHandler)
}

abstract class AbstractForeignAnnotationsTestBase(
    private val configureFrontend: TestConfigurationBuilder.() -> Unit = configureClassicFrontend
) : AbstractKotlinCompilerTest() {

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
            +WITH_FOREIGN_ANNOTATIONS
        }

        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmForeignAnnotationsConfigurator,
            ::JvmEnvironmentConfigurator
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        configureFrontend()

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Annotations
                +WITH_JSR305_TEST_ANNOTATIONS
            }
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java8Tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Java8Annotations
            }
        }

        forTestsMatching("compiler/testData/diagnostics/foreignAnnotationsTests/java9Tests/*") {
            defaultDirectives {
                ANNOTATIONS_PATH with JavaForeignAnnotationType.Java9Annotations
                JDK_KIND with TestJdkKind.FULL_JDK_9
                COMPILE_JAVA_USING with TestJavacVersion.JAVAC_9
            }
        }
    }
}

abstract class AbstractForeignAnnotationsSourceJavaTest : AbstractForeignAnnotationsTestBase()

abstract class AbstractForeignAnnotationsCompiledJavaTest : AbstractForeignAnnotationsTestBase {
    constructor() : super()
    constructor(configureFrontend: TestConfigurationBuilder.() -> Unit) : super(configureFrontend)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ALL_JAVA_AS_BINARY
                +SKIP_TXT
            }

            useMetaTestConfigurators(::ForeignAnnotationAgainstCompiledJavaTestSuppressor)
        }
    }
}

abstract class AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest : AbstractForeignAnnotationsCompiledJavaTest {
    constructor() : super()
    constructor(configureFrontend: TestConfigurationBuilder.() -> Unit) : super(configureFrontend)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +USE_PSI_CLASS_FILES_READING
            }

            useMetaTestConfigurators(::PsiClassFilesReadingForCompiledJavaTestSuppressor)
        }
    }
}
