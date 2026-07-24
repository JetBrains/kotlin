/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.junit.jupiter.api.Test

/**
 * WARNING TO KOTLIN COMPILER DEVELOPERS:
 *
 * DO NOT RENAME, REMOVE, OR SIGNIFICANTLY MODIFY THE CLASSES/MEMBERS REFERENCED IN THIS TEST.
 *
 * Several downstream compiler wrappers, pipeline extensions, and integration tools depend on
 * these specific internal phases, CLI pipelines, compiler arguments, and plugin extension points
 * to coordinate compilation and custom plugins. Changing these without deprecation will
 * break downstream compiler wrappers and integrations.
 *
 * IF YOU NEED TO CHANGE THESE:
 * 1. Rather than renaming/removing, please mark the API as @Deprecated first.
 * 2. Keep the deprecated implementation functional or provide a backward-compatible pathway.
 * 3. Coordinate with downstream maintainers to allow updating custom wrappers before
 *    these APIs are fully removed.
 */
class CompilerPipelineDependencyTest {

    @Suppress("PrivatePropertyName")
    private val WARNING_MESSAGE = """
        =============================================================================
        CRITICAL COMPILER API BREAKAGE DETECTED!
        
        A class, property, or method used by downstream Kotlin JVM compiler wrappers
        or compiler plugin integrations has been renamed, removed, or modified in 
        an incompatible way.
        
        To resolve this:
        1. Keep the old signature / class as @Deprecated to preserve compatibility, OR
        2. Coordinate with downstream teams to migrate wrapper code in sync.
        =============================================================================
    """.trimIndent()

    private val criticalClasses = listOf(
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments",
        "org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline",
        "org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors",
        $$"org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors$CheckDiagnosticCollector",
        "org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase",
        "org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase",
        "org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase",
        "org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase",
        "org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase",
        "org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector",
        "org.jetbrains.kotlin.cli.common.messages.MessageRenderer",
        "org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity",
        "org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation",
        "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
        "org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
        "org.jetbrains.kotlin.compiler.plugin.AbstractCliOption",
        "org.jetbrains.kotlin.compiler.plugin.CliOption",
        "org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException",
        "org.jetbrains.kotlin.config.CompilerConfiguration",
        "org.jetbrains.kotlin.config.CommonConfigurationKeys",
        "org.jetbrains.kotlin.config.Services",
        "org.jetbrains.kotlin.cli.common.ExitCode",
        "org.jetbrains.kotlin.platform.jvm.JvmPlatforms",
        "org.jetbrains.kotlin.util.PerformanceManagerImpl",
        "org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension",
        "org.jetbrains.kotlin.backend.common.extensions.IrPluginContext",
        "org.jetbrains.kotlin.diagnostics.PendingDiagnosticReporter"
    )

    private val criticalFields: Map<String, Map<String, String>> = mapOf(
        "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments" to mapOf(
            "friendPaths" to "[Ljava.lang.String;", // Array<String> in JVM representation
            "pluginClasspaths" to "[Ljava.lang.String;",
            "destination" to "java.lang.String"
        )
    )

    private val criticalMethods: Map<String, Map<String, List<String>>> = mapOf(
        "org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline" to mapOf(
            "createCompoundPhase" to listOf("org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments"),
            "isKaptMode" to listOf("org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments"),
            "getDefaultPerformanceManager" to emptyList()
        )
    )

    @Test
    fun testCriticalClassesExist() {
        for (className in criticalClasses) {
            assertClassExists(className)
        }
    }

    @Test
    fun testCriticalFieldsAndMethods() {
        for ([className, fields] in criticalFields) {
            for ([fieldName, expectedType] in fields) {
                assertFieldExists(className, fieldName, expectedType)
            }
        }

        for ([className, methods] in criticalMethods) {
            for ([methodName, parameterTypes] in methods) {
                assertMethodExists(className, methodName, parameterTypes)
            }
        }
    }

    @Test
    fun testReflectionFailureCases() {
        // 1. Verify ClassNotFoundException triggers AssertionError with a warning message
        try {
            assertClassExists("org.jetbrains.kotlin.cli.SomeNonExistingClass")
            throw AssertionError("Expected AssertionError was not thrown for non-existing class.")
        } catch (e: AssertionError) {
            assertWarningMessagePresent(e)
        }

        // 2. Verify NoSuchFieldException triggers AssertionError with a warning message
        try {
            assertFieldExists(
                "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments",
                "nonExistingField",
                "java.lang.String"
            )
            throw AssertionError("Expected AssertionError was not thrown for non-existing field.")
        } catch (e: AssertionError) {
            assertWarningMessagePresent(e)
        }

        // 3. Verify an incompatible field type triggers AssertionError with a warning message
        try {
            assertFieldExists(
                "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments",
                "destination",
                "java.lang.Integer" // destination is String, not Integer
            )
            throw AssertionError("Expected AssertionError was not thrown for invalid field type.")
        } catch (e: AssertionError) {
            assertWarningMessagePresent(e)
        }

        // 4. Verify NoSuchMethodException triggers AssertionError with a warning message
        try {
            assertMethodExists(
                "org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline",
                "nonExistingMethod",
                emptyList()
            )
            throw AssertionError("Expected AssertionError was not thrown for non-existing method.")
        } catch (e: AssertionError) {
            assertWarningMessagePresent(e)
        }
    }

    private fun assertWarningMessagePresent(e: AssertionError) {
        val message = e.message ?: throw AssertionError("AssertionError had null message", e)
        if (!message.contains("CRITICAL COMPILER API BREAKAGE DETECTED")) {
            throw AssertionError("AssertionError did not contain the critical API breakage warning: $message", e)
        }
    }

    private fun assertClassExists(fqn: String) {
        try {
            Class.forName(fqn)
        } catch (e: ClassNotFoundException) {
            throw AssertionError("CRITICAL API BREAKAGE: Class $fqn not found.\n$WARNING_MESSAGE", e)
        }
    }

    private fun assertFieldExists(className: String, fieldName: String, expectedTypeFqn: String) {
        val clazz = Class.forName(className)
        val expectedType = Class.forName(expectedTypeFqn)
        try {
            val field = clazz.getDeclaredField(fieldName)
            if (!expectedType.isAssignableFrom(field.type)) {
                throw AssertionError("CRITICAL API BREAKAGE: Field $fieldName in class $className has type ${field.type.name}, expected $expectedTypeFqn.\n$WARNING_MESSAGE")
            }
        } catch (e: NoSuchFieldException) {
            // Check if it's a property with getter/setter instead of public field
            val getterName = "get" + fieldName.replaceFirstChar { it.uppercase() }
            try {
                val method = clazz.getMethod(getterName)
                if (!expectedType.isAssignableFrom(method.returnType)) {
                    throw AssertionError("CRITICAL API BREAKAGE: Property getter $getterName in class $className has return type ${method.returnType.name}, expected $expectedTypeFqn.\n$WARNING_MESSAGE")
                }
            } catch (_: NoSuchMethodException) {
                throw AssertionError(
                    "CRITICAL API BREAKAGE: Field $fieldName (or getter $getterName) not found in class $className.\n$WARNING_MESSAGE",
                    e
                )
            }
        }
    }

    private fun assertMethodExists(className: String, methodName: String, parameterTypesFqNames: List<String>) {
        val clazz = Class.forName(className)
        val parameterTypes = parameterTypesFqNames.map { Class.forName(it) }

        var currentClazz: Class<*>? = clazz
        var method: java.lang.reflect.Method? = null
        while (currentClazz != null) {
            try {
                method = currentClazz.getDeclaredMethod(methodName, *parameterTypes.toTypedArray())
                break // found it
            } catch (_: NoSuchMethodException) {
                currentClazz = currentClazz.superclass
            }
        }

        if (method == null) {
            // Try to find a method with the same name and compatible parameter types (handling generic erasure / subtyping)
            currentClazz = clazz
            var match: java.lang.reflect.Method? = null
            while (currentClazz != null) {
                val methods = currentClazz.declaredMethods.filter { it.name == methodName && it.parameterCount == parameterTypes.size }
                match = methods.firstOrNull { m ->
                    m.parameterTypes.zip(parameterTypes).all { [paramType, expectedType] ->
                        paramType.isAssignableFrom(expectedType)
                    }
                }
                if (match != null) break
                currentClazz = currentClazz.superclass
            }

            if (match == null) {
                val paramsStr = parameterTypesFqNames.joinToString()
                throw AssertionError("CRITICAL API BREAKAGE: Method $methodName($paramsStr) not found in class hierarchy of $className.\n$WARNING_MESSAGE")
            }
        }
    }
}
