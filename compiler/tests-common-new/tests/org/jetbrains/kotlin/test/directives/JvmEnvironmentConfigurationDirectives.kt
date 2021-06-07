/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.TestJavacVersion
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object JvmEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    @Suppress("RemoveExplicitTypeArguments")
    val JVM_TARGET by enumDirective<JvmTarget>(
        description = "Target bytecode version",
        additionalParser = JvmTarget.Companion::fromString
    )

    val JDK_KIND by enumDirective<TestJdkKind>("JDK used in tests")
    val FULL_JDK by directive("Add full java standard library to classpath")
    val STDLIB_JDK8 by directive("Add Java 8 stdlib to classpath")

    val WITH_RUNTIME by directive(
        description = """
            Add Kotlin stdlib to classpath
            This directive is deprecated, use WITH_STDLIB instead
        """.trimIndent()
    )
    val WITH_STDLIB by directive("Add Kotlin runtime to classpath")
    val WITH_REFLECT by directive("Add Kotlin reflect to classpath")
    val NO_RUNTIME by directive("Don't add any runtime libs to classpath")

    val WITH_FOREIGN_ANNOTATIONS by directive("Add foreign nullability annotations to classpath")

    val WITH_JSR305_TEST_ANNOTATIONS by directive(
        description = """
            Add test nullability annotations based on JSR-305 annotations
            See directory ./compiler/testData/diagnostics/helpers/jsr305_test_annotations
        """.trimIndent()
    )

    val USE_PSI_CLASS_FILES_READING by directive("Use a slower (PSI-based) class files reading implementation")
    val USE_JAVAC by directive("Enable javac integration")
    val SKIP_JAVA_SOURCES by directive("Don't add java sources to compile classpath")
    val INCLUDE_JAVA_AS_BINARY by directive(
        "Compile this java file into jar and add it to classpath instead of compiling Kotlin with Java sources",
        applicability = DirectiveApplicability.File
    )
    val ALL_JAVA_AS_BINARY by directive(
        "Compile all java files into jar and add it to classpath instead of compiling Kotlin with Java sources",
        applicability = DirectiveApplicability.Global
    )
    val COMPILE_JAVA_USING by enumDirective<TestJavacVersion>(
        "Compile all including java files using javac of specific version",
        applicability = DirectiveApplicability.Global
    )

    @Suppress("RemoveExplicitTypeArguments")
    val STRING_CONCAT by enumDirective<JvmStringConcat>(
        description = "Configure mode of string concatenation",
        additionalParser = JvmStringConcat.Companion::fromString
    )

    @Suppress("RemoveExplicitTypeArguments")
    val ASSERTIONS_MODE by enumDirective<JVMAssertionsMode>(
        description = "Configure jvm assertions mode",
        additionalParser = JVMAssertionsMode.Companion::fromString
    )

    @Suppress("RemoveExplicitTypeArguments")
    val CONSTRUCTOR_CALL_NORMALIZATION_MODE by enumDirective<JVMConstructorCallNormalizationMode>(
        description = "Configure jvm constructor call normalization mode",
        additionalParser = JVMConstructorCallNormalizationMode.Companion::fromStringOrNull
    )

    val SAM_CONVERSIONS by enumDirective(
        description = "SAM conversion code generation scheme",
        additionalParser = JvmClosureGenerationScheme.Companion::fromString
    )

    val LAMBDAS by enumDirective(
        description = "Lambdas code generation scheme",
        additionalParser = JvmClosureGenerationScheme.Companion::fromString
    )

    val USE_OLD_INLINE_CLASSES_MANGLING_SCHEME by directive(
        description = "Enable old mangling scheme for inline classes"
    )
}
