/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class) @file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.*
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import java.nio.file.Path
import kotlin.emptyArray

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.5.0.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("ClassName")
internal class KotlinWrapperPre2_5_0(
    private val base: KotlinToolchains,
) : KotlinToolchains by base {

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        JsPlatformToolchain::class.java -> JsPlatformToolchainWrapper(base.getToolchain(type))
        WasmPlatformToolchain::class.java -> WasmPlatformToolchainWrapper(base.getToolchain(type))
        KotlinMetadataPlatformToolchain::class.java -> KotlinMetadataPlatformToolchainWrapper(base.getToolchain(type))
        else -> base.getToolchain(type)
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(this, base.createBuildSession())
    }

    class BuildSessionWrapper(override val kotlinToolchains: KotlinWrapperPre2_5_0, private val base: KotlinToolchains.BuildSession) :
        KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            // Unwrap so the pre-2.4.0 executeOperation can handle its own type check
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation.Builder = JvmCompilationOperationBuilderWrapper(
            base.jvmCompilationOperationBuilder(sources, destinationDirectory),
        )
    }

    private class JvmCompilationOperationBuilderWrapper(
        private val base: JvmCompilationOperation.Builder,
    ) : JvmCompilationOperation.Builder by base {
        override val compilerArguments: JvmCompilerArguments.Builder = JvmCompilerArgumentsBuilderWrapper(base.compilerArguments)

        override fun build(): JvmCompilationOperation {
            return JvmCompilationOperationWrapper(
                base.build()
            )
        }
    }

    private class JvmCompilationOperationWrapper(
        private val base: JvmCompilationOperation,
    ) : JvmCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {

        override fun toBuilder(): JvmCompilationOperation.Builder = JvmCompilationOperationBuilderWrapper(base.toBuilder())
    }

    internal class JvmCompilerArgumentsBuilderWrapper(
        private val base: JvmCompilerArguments.Builder,
    ) : JvmCompilerArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) {
            applyCommandLineArgumentsImpl(base, arguments)
        }
    }

    private class JsPlatformToolchainWrapper(private val base: JsPlatformToolchain) : JsPlatformToolchain by base {
        override fun jsKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): JsKlibCompilationOperation.Builder =
            JsKlibCompilationOperationBuilderWrapper(base.jsKlibCompilationOperationBuilder(sources, destination))

        override fun jsLinkingOperationBuilder(klib: Path, destination: Path): JsLinkingOperation.Builder =
            JsLinkingOperationBuilderWrapper(base.jsLinkingOperationBuilder(klib, destination))
    }

    private class JsKlibCompilationOperationBuilderWrapper(private val base: JsKlibCompilationOperation.Builder) :
        JsKlibCompilationOperation.Builder by base {
        override val compilerArguments: JsCompilerKlibArguments.Builder = JsKlibCompilerArgumentsBuilderWrapper(base.compilerArguments)
        override fun build() = JsKlibCompilationOperationWrapper(base.build())
    }

    private class JsKlibCompilationOperationWrapper(private val base: JsKlibCompilationOperation) : JsKlibCompilationOperation by base,
        BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder() = JsKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class JsKlibCompilerArgumentsBuilderWrapper(private val base: JsCompilerKlibArguments.Builder) :
        JsCompilerKlibArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) = applyCommandLineArgumentsImpl(base, arguments)
    }

    private class JsLinkingOperationBuilderWrapper(private val base: JsLinkingOperation.Builder) : JsLinkingOperation.Builder by base {
        override val compilerArguments: JsCompilerLinkingArguments.Builder =
            JsLinkingCompilerArgumentsBuilderWrapper(base.compilerArguments)

        override fun build() = JsLinkingOperationWrapper(base.build())
    }

    private class JsLinkingOperationWrapper(private val base: JsLinkingOperation) : JsLinkingOperation by base,
        BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder() = JsLinkingOperationBuilderWrapper(base.toBuilder())
    }

    private class JsLinkingCompilerArgumentsBuilderWrapper(private val base: JsCompilerLinkingArguments.Builder) :
        JsCompilerLinkingArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) = applyCommandLineArgumentsImpl(base, arguments)
    }

    private class WasmPlatformToolchainWrapper(private val base: WasmPlatformToolchain) : WasmPlatformToolchain by base {
        override fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): WasmKlibCompilationOperation.Builder =
            WasmKlibCompilationOperationBuilderWrapper(base.wasmKlibCompilationOperationBuilder(sources, destination))

        override fun wasmLinkingOperationBuilder(klib: Path, destination: Path): WasmLinkingOperation.Builder =
            WasmLinkingOperationBuilderWrapper(base.wasmLinkingOperationBuilder(klib, destination))
    }

    private class WasmKlibCompilationOperationBuilderWrapper(private val base: WasmKlibCompilationOperation.Builder) :
        WasmKlibCompilationOperation.Builder by base {
        override val compilerArguments: WasmCompilerKlibArguments.Builder = WasmKlibCompilerArgumentsBuilderWrapper(base.compilerArguments)
        override fun build() = WasmKlibCompilationOperationWrapper(base.build())
    }

    private class WasmKlibCompilationOperationWrapper(private val base: WasmKlibCompilationOperation) :
        WasmKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder() = WasmKlibCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class WasmKlibCompilerArgumentsBuilderWrapper(private val base: WasmCompilerKlibArguments.Builder) :
        WasmCompilerKlibArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) = applyCommandLineArgumentsImpl(base, arguments)
    }

    private class WasmLinkingOperationBuilderWrapper(private val base: WasmLinkingOperation.Builder) :
        WasmLinkingOperation.Builder by base {
        override val compilerArguments: WasmCompilerLinkingArguments.Builder =
            WasmLinkingCompilerArgumentsBuilderWrapper(base.compilerArguments)

        override fun build() = WasmLinkingOperationWrapper(base.build())
    }

    private class WasmLinkingOperationWrapper(private val base: WasmLinkingOperation) : WasmLinkingOperation by base,
        BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder() = WasmLinkingOperationBuilderWrapper(base.toBuilder())
    }

    private class WasmLinkingCompilerArgumentsBuilderWrapper(private val base: WasmCompilerLinkingArguments.Builder) :
        WasmCompilerLinkingArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) = applyCommandLineArgumentsImpl(base, arguments)
    }

    private class KotlinMetadataPlatformToolchainWrapper(private val base: KotlinMetadataPlatformToolchain) :
        KotlinMetadataPlatformToolchain by base {
        override fun metadataKlibCompilationOperationBuilder(
            sources: List<Path>,
            destination: Path,
        ): KotlinMetadataKlibCompilationOperation.Builder =
            KotlinMetadataCompilationOperationBuilderWrapper(base.metadataKlibCompilationOperationBuilder(sources, destination))
    }

    private class KotlinMetadataCompilationOperationBuilderWrapper(private val base: KotlinMetadataKlibCompilationOperation.Builder) :
        KotlinMetadataKlibCompilationOperation.Builder by base {
        override val compilerArguments: MetadataArguments.Builder = KotlinMetadataCompilerArgumentsBuilderWrapper(base.compilerArguments)
        override fun build() = KotlinMetadataCompilationOperationWrapper(base.build())
    }

    private class KotlinMetadataCompilationOperationWrapper(private val base: KotlinMetadataKlibCompilationOperation) :
        KotlinMetadataKlibCompilationOperation by base, BuildOperationWrapper<CompilationResult>(base) {
        override fun toBuilder() = KotlinMetadataCompilationOperationBuilderWrapper(base.toBuilder())
    }

    private class KotlinMetadataCompilerArgumentsBuilderWrapper(private val base: MetadataArguments.Builder) :
        MetadataArguments.Builder by base {
        override fun applyCommandLineArguments(arguments: List<String>) = applyCommandLineArgumentsImpl(base, arguments)
    }
}

private fun CommonCompilerArguments.Builder.toCompilerArguments(): Any {
    var current: Any = this
    while (true) {
        val arguments =
            // > 2.4.20
            current::class.java.methods.firstOrNull { it.name == "toCompilerArguments" && it.parameterCount == 0 }?.invoke(current)
            // < 2.4.20
                ?: current::class.java.methods.firstOrNull { it.name == "toCompilerArguments" && it.parameterCount == 1 }?.let { method ->
                    val arguments = method.parameterTypes[0].getDeclaredConstructor().newInstance()
                    unwrapInvocationTargetException { method.invoke(current, arguments) }
                    arguments
                }
        if (arguments != null) return arguments
        current = current::class.java.getDeclaredField("base").also { it.isAccessible = true }.get(current)
    }
}

private fun applyCommandLineArgumentsImpl(base: CommonCompilerArguments.Builder, arguments: List<String>) {
    val compilerArgs = base.toCompilerArguments()
    val compilerArgsClass = compilerArgs.javaClass
    val parseCommandLineArgumentsClass =
        compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt")

    fun parseCommandLineArguments(arguments: List<String>, compilerArgs: Any, overrideArguments: Boolean) {
        parseCommandLineArgumentsClass.getMethod(
            "parseCommandLineArguments",
            List::class.java,
            compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments"),
            Boolean::class.java
        ).invoke(null, arguments, compilerArgs, overrideArguments)
    }

    fun toArgumentStrings(compilerArgs: Any, shortArgumentKeys: Boolean, compactArgumentValues: Boolean): List<String> {
        @Suppress("UNCHECKED_CAST")
        return compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.compilerRunner.ArgumentsToStrings").getMethod(
            "toArgumentStrings",
            compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments"),
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
        ).invoke(null, compilerArgs, shortArgumentKeys, compactArgumentValues) as List<String>
    }

    // validateArgumentsAllErrors(errors: ArgumentParseErrors?): List<String>
    fun validateArgumentsAllErrors(errors: Any?): List<String> {
        @Suppress("UNCHECKED_CAST")
        return parseCommandLineArgumentsClass.getMethod(
            "validateArgumentsAllErrors",
            compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors"),
        ).invoke(null, errors) as List<String>
    }

    // validateArguments(errors: ArgumentParseErrors?): String?
    fun validateArguments(errors: Any?): String? {
        @Suppress("UNCHECKED_CAST")
        return parseCommandLineArgumentsClass.getMethod(
            "validateArguments",
            compilerArgsClass.classLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors"),
        ).invoke(null, errors) as String?
    }

    fun containsAnyPluginsRelatedArguments(arguments: List<String>): Boolean = setOf(
        "-Xplugin", "-P", "-Xcompiler-plugin-order"
    ).any { prefix -> arguments.any { it == prefix || it.startsWith("$prefix=") } }

    if (!containsAnyPluginsRelatedArguments(arguments)) {
        // The caller is not setting any plugin-related arguments, so we need to clear the existing ones to avoid duplication.
        // If we didn't clear them here, they'd be set in `applyArgumentStrings` below and later _again_ from `this[COMPILER_PLUGINS]`
        // when running the compilation.
        compilerArgsClass.getMethod("setPluginClasspaths", Array<String>::class.java).invoke(compilerArgs, emptyArray<String>())
        compilerArgsClass.getMethod("setPluginOptions", Array<String>::class.java).invoke(compilerArgs, emptyArray<String>())
        compilerArgsClass.getMethod("setPluginOrderConstraints", Array<String>::class.java).invoke(compilerArgs, emptyArray<String>())
    }

    parseCommandLineArguments(arguments, compilerArgs, true)

    //propagate errors
    findArgumentValidationErrorsSet(base)?.let {
        validateArgumentsAllErrors(findErrors(compilerArgs)).forEach { error -> it.add(error) }
    } ?: validateArguments(findErrors(compilerArgs))?.let { throw CompilerArgumentsParseException(it) }

    @Suppress("UNCHECKED_CAST") val argumentStrings = toArgumentStrings(
        compilerArgs,
        shortArgumentKeys = false,
        compactArgumentValues = true
    )
    if (containsAnyPluginsRelatedArguments(arguments)) {
        // The caller is overriding at least some plugin-related arguments, so we need to clear the existing ones, as it's
        // impossible to guess the new COMPILER_PLUGINS value from the arguments (the transformation is not reversible).
        // Later, inside `applyArgumentStrings` (below), COMPILER_PLUGINS will be set to the "RAW_PLUGIN_ID" value, indicating
        // that the raw values of compiler related arguments must be considered.
        try {
            base[CommonCompilerArguments.COMPILER_PLUGINS] = emptyList()
        } catch (_: Exception) {
            // some older compiler versions don't support COMPILER_PLUGINS
        }
    }
    base.applyArgumentStrings(argumentStrings)
}

private fun findArgumentValidationErrorsSet(current: Any, currentClass: Class<*> = current::class.java): MutableSet<String>? {
    return try {
        @Suppress("UNCHECKED_CAST")
        currentClass.getDeclaredField("_argumentValidationErrors").also { it.isAccessible = true }.get(current) as MutableSet<String>
    } catch (_: Exception) {
        null
    } ?: try {
        findArgumentValidationErrorsSet(current::class.java.getDeclaredField("base").also { it.isAccessible = true }.get(current))
    } catch (_: Exception) {
        null
    } ?: currentClass.superclass?.let { superClass ->
        findArgumentValidationErrorsSet(current, superClass)
    }
}

private fun findErrors(current: Any, currentClass: Class<*> = current::class.java): Any? {
    return try {
        @Suppress("UNCHECKED_CAST")
        currentClass.getDeclaredField("errors").also { it.isAccessible = true }.get(current)
    } catch (_: Exception) {
        null
    } ?: try {
        currentClass.superclass?.let { superClass ->
            findErrors(current, superClass)
        }
    } catch (_: Exception) {
        null
    }
}
