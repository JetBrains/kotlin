/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object JvmEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    val JVM_TARGET by enumDirective(
        description = "Target bytecode version",
        additionalParser = JvmTarget.Companion::fromString
    )

    val JDK_KIND by enumDirective<TestJdkKind>("JDK used in tests")
    val FULL_JDK by directive("Add full java standard library to classpath")
    val STDLIB_JDK8 by directive("Add Java 8 stdlib to classpath")

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

    val USE_JAVAC by directive("Enable javac integration (has no effect on non-K1 tests)")
    val JAVAC_EXPECTED_FILE by directive("Dump descriptors to .javac.txt file if $USE_JAVAC is enabled")
    val SKIP_JAVAC by directive("Skip the test if $USE_JAVAC is enabled")

    val PROVIDE_JAVA_AS_BINARIES by directive(
        "Compile Kotlin with classpath JAR binaries for all .java sources instead of with .java sources themselves"
    )

    val STRING_CONCAT by enumDirective(
        description = "Configure mode of string concatenation",
        additionalParser = JvmStringConcat.Companion::fromString
    )

    val ASSERTIONS_MODE by enumDirective(
        description = "Configure jvm assertions mode",
        additionalParser = JVMAssertionsMode.Companion::fromString
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

    val SERIALIZE_IR by enumDirective(
        description = "Enable serialization of JVM IR",
        additionalParser = JvmSerializeIrMode.Companion::fromString
    )

    val ENABLE_DEBUG_MODE by directive("Enable debug mode for compilation")
}
