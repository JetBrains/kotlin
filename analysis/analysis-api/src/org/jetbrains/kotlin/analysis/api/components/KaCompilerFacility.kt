/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.compile.KaCodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaCompilerFacility : KaSessionComponent {
    /**
     * Compiles the given [file] in-memory using the specified [options].
     *
     * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException]. The implementation should wrap the
     * `compile()` call into a `try`/`catch` block when necessary.
     *
     * @param file A file to compile.
     *  The file must be either a source module file, or a [KtCodeFragment].
     *  For a [KtCodeFragment], a source module context, a compiled library source context, or an empty context (`null`) are supported.
     *
     * @param options The compilation options created via [createCompilationOptions].
     *
     * @see createCompilationOptions
     */
    @KaExperimentalApi
    @Throws(KaCodeCompilationException::class)
    public fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult

    /**
     * Creates a new [KaCompilationOptions] instance using the given DSL [init] block.
     *
     * Example usage:
     * ```kotlin
     * val options = createCompilationOptions {
     *     target(KaCompilationTarget.JVM)
     *     moduleName("myModule")
     *     languageVersionSettings(languageSettings)
     *     allowedErrorFilter { diagnostic -> diagnostic.factoryName in allowedErrors }
     * }
     * ```
     *
     * @param init A lambda that configures the [KaCompilationOptionsBuilder].
     */
    @KaExperimentalApi
    public fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions

    /**
     * Creates a copy of these [KaCompilationOptions], applying the given [init] modifications.
     *
     * Options not explicitly overridden in [init] retain their original values.
     *
     * @param init A lambda that configures the [KaCompilationOptionsBuilder] with modifications to apply.
     */
    @KaExperimentalApi
    public fun KaCompilationOptions.modify(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptions] instead.**
 *
 * An immutable set of options for in-memory compilation via [KaCompilerFacility].
 *
 * Use [KaCompilerFacility.createCompilationOptions] to create an instance, and [KaCompilerFacility.modify]
 * to produce a modified copy.
 *
 * @see KaCompilationOptionsBuilder
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilationOptions : org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptions

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptionsBuilder] instead.**
 *
 * A DSL builder for [KaCompilationOptions].
 *
 * Provides methods to configure the compilation target, language settings, JVM-specific options, error handling, and code fragment
 * parameters. Instances are created internally by [KaCompilerFacility.createCompilationOptions] and
 * [KaCompilerFacility.modify].
 *
 * @see KaCompilationOptions
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilationOptionsBuilder : org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptionsBuilder {
    /** Sets the target platform for compilation. Must be provided. */
    public fun target(value: KaCompilationTarget)

    /** Sets the module name used for the compiled output. */
    override fun moduleName(value: String)

    /**
     * Sets a custom actualizer for common source modules in multiplatform projects.
     *
     * @see KaCompilerFacilityModuleActualizer
     */
    public fun moduleActualizer(value: KaCompilerFacilityModuleActualizer)

    /** Sets the language version settings used during compilation. */
    override fun languageVersionSettings(value: LanguageVersionSettings)

    /**
     * Sets a filter for allowed errors. Compilation will be aborted if there are errors that this filter rejects.
     *
     * A filter returning `true` means the error is allowed and will not abort compilation.
     * Defaults to rejecting all errors.
     */
    override fun allowedErrorFilter(value: (KaDiagnostic) -> Boolean)

    /**
     * Sets the simple class name for the code fragment facade class.
     * Only relevant when compiling [KtCodeFragment] files.
     */
    override fun codeFragmentClassName(value: String)

    /**
     * Sets the entry point method name for the code fragment.
     * Only relevant when compiling [KtCodeFragment] files.
     */
    override fun codeFragmentMethodName(value: String)

    /** Sets the JVM bytecode target version. */
    override fun jvmTarget(value: KaJvmTarget)

    /**
     * Sets a handler which is called whenever a new class file is produced.
     *
     * @see KaCompiledClassHandler
     */
    public fun jvmCompiledClassHandler(value: KaCompiledClassHandler)

    /** Enables output of ASM bytecode listing. When `true`, selects the test-mode class builder factory. */
    @KaNonPublicApi
    override fun jvmOutputAsmListing(value: Boolean)

    /**
     * Whether unbound IR symbols should be stubbed instead of linked.
     *
     * This should be enabled if the compiled file could refer to symbols defined in another file of the same module.
     */
    @KaIdeApi
    override fun stubUnboundIrSymbols(value: Boolean)

    /** Disables inlining of `inline` functions. */
    @KaIdeApi
    override fun disableInline(value: Boolean)

    /** Disables null-check assertions on receiver and arguments of platform-typed calls. */
    @KaIdeApi
    override fun disableCallAssertions(value: Boolean)

    /** Disables optimizations in generated code. */
    @KaIdeApi
    override fun disableOptimization(value: Boolean)

    /** Disables null-check assertions on parameters. */
    @KaIdeApi
    override fun disableParameterAssertions(value: Boolean)

    /** Ignores errors from constant expression optimization. */
    @KaIdeApi
    override fun ignoreConstOptimizationErrors(value: Boolean)

    /**
     * Sets the execution stack for debugger code fragment compilation.
     *
     * A sequence of PSI elements representing function calls or property accesses in the current execution stack,
     * listed from the top to the bottom.
     */
    @KaIdeApi
    override fun jvmExecutionStack(value: Sequence<PsiElement?>)

    /** Enables generation of parameter metadata (names and access flags) in class files. */
    @KaIdeApi
    override fun jvmGenerateParameterMetadata(value: Boolean)

    /** Uses `invokedynamic` for SAM conversions instead of generating anonymous classes. */
    @KaIdeApi
    override fun jvmUseInvokeDynamicForSamConversions(value: Boolean)

    /** Uses `invokedynamic` for lambdas instead of generating anonymous classes. */
    @KaIdeApi
    override fun jvmUseInvokeDynamicForLambdas(value: Boolean)
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompilationResult] instead.**
 *
 * An in-memory compilation result returned from [KaCompilerFacility].
 *
 * Compilation fails if there are critical errors reported either on the frontend or on the backend side.
 * Keep in mind that [KaCompilationResult] is a part of the Analysis API, so it should only be used inside an
 * [analysis block][org.jetbrains.kotlin.analysis.api.session.analyze].
 */
@KaObsoleteComponentApi
@KaExperimentalApi
public sealed class KaCompilationResult(
    /** A list of exceptions that were thrown during compilation but workaround somehow */
    public val mutedExceptions: List<Throwable>,
) {
    /**
     * A successful compilation result.
     *
     * @property output Output files produced by the compiler. For the JVM target, these are class files and '.kotlin_module'.
     * @property capturedValues Context values captured by a [KtCodeFragment]. Empty for an ordinary [KtFile].
     * @property canBeCached When the flag is raised, this compilation result is safe to cache and avoid re-compilation
     *  Suppose we have the following code and evaluating `T::class`
     *
     *  inline fun <reified T> foo() {
     *      //Breakpoint!
     *      println()
     *  }
     *
     *  fun main() {
     *      foo<Int>()
     *      foo<String>()
     *  }
     *
     *  We should emit different bytecode in <Int> and <String> calls, yet we are at the same line and compiling the same code.
     */
    @KaExperimentalApi
    public class Success(
        public val output: List<KaCompiledFile>,
        public val capturedValues: List<KaCodeFragmentCapturedValue>,
        public var canBeCached: Boolean,
        mutedExceptions: List<Throwable> = emptyList(),
    ) : KaCompilationResult(mutedExceptions)

    /**
     * A failed compilation result.
     *
     * @property errors Non-recoverable errors which occurred either during code analysis or during code generation.
     */
    @KaExperimentalApi
    public class Failure(
        public val errors: List<KaDiagnostic>,
        mutedExceptions: List<Throwable> = emptyList(),
    ) : KaCompilationResult(mutedExceptions)
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompiledFile] instead.**
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompiledFile : org.jetbrains.kotlin.analysis.api.compilation.KaCompiledFile {
    /**
     * The path of the compiled file relative to the root of the output directory.
     */
    override val path: String

    /**
     * The source files that were compiled to produce this file.
     */
    override val sourceFiles: List<File>

    /**
     * The content of the compiled file.
     */
    override val content: ByteArray
}

/**
 * Whether the compiled file is a Java class file.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.compilation' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.isClassFile",
        "org.jetbrains.kotlin.analysis.api.compilation.isClassFile",
    ),
)
@KaExperimentalApi
public val KaCompiledFile.isClassFile: Boolean
    get() = path.endsWith(".class", ignoreCase = true)

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompilationTarget] instead.**
 *
 * The target platform of the compilation performed by [KaCompilerFacility].
 */
@KaObsoleteComponentApi
@KaExperimentalApi
public enum class KaCompilationTarget {
    /**
     * JVM target (produces '.class' files).
     */
    JVM,
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompiledClassHandler] instead.**
 *
 * A handler which is called whenever a new class file is produced, when compiling sources to the JVM target.
 *
 * @see KaCompilationTarget.JVM
 */
@KaObsoleteComponentApi
@KaSpi
@KaExperimentalApi
public fun interface KaCompiledClassHandler : org.jetbrains.kotlin.analysis.api.compilation.KaCompiledClassHandler {
    /**
     * [handleClassDefinition] is called whenever a new class file is produced.
     *
     * @param file The [PsiFile] containing the class definition. It can be `null` when the generated class file has no PSI file in sources,
     *  for example if it's an anonymous object from another module, regenerated during inlining.
     * @param className The name of the class in the JVM's internal name format, for example `"java/lang/Object"`.
     */
    @KaSpiExtensionPoint
    override fun handleClassDefinition(file: PsiFile?, className: String)
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCodeCompilationException] instead.**
 *
 * Thrown when an exception occurred while analyzing the code to be compiled, or during target platform code generation.
 *
 * @see KaCompilerFacility
 */
@KaObsoleteComponentApi
@KaExperimentalApi
public class KaCodeCompilationException(cause: Throwable) : RuntimeException(cause)

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.compilation.KaCompilerFacilityModuleActualizer] instead.**
 *
 * Actualizer for common source modules.
 *
 * The Kotlin compiler cannot directly compile classes from common modules, as it needs dependencies and language settings from the target
 * platform. Such as, even if the compiled class only uses 'kotlin-stdlib', the JVM compiler still needs the library bytecode to understand
 * JVM facade names and to be able to inline functions (the JVM inliner uses Java bytecode instead of the serialized IR).
 *
 * [KaCompilerFacility] attempts to find the platform module with an appropriate target by itself and substitutes it instead of the original
 * common module – that way, it can pass all the required information to the compiler. However, there might be multiple platform modules
 * (e.g., Android and JVM); in that case, the facility chooses the first matching one. [KaCompilerFacilityModuleActualizer] is a way to
 * override the default behavior by offering a closer match – e.g., a module with an Android target.
 */
@KaObsoleteComponentApi
@KaExperimentalApi
@KaSpi
public fun interface KaCompilerFacilityModuleActualizer {
    /**
     * Actualizes the [module] with the common multiplatform target.
     * Returns an actual counterpart of [module], target of which matches the [target], or `null` if such a module does not exist.
     */
    @KaSpiExtensionPoint
    public fun actualize(module: KaModule, target: KaCompilationTarget): KaModule?
}

/**
 * Compiles the given [file] in-memory using the specified [options].
 *
 * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException]. The implementation should wrap the
 * `compile()` call into a `try`/`catch` block when necessary.
 *
 * @param file A file to compile.
 *  The file must be either a source module file, or a [KtCodeFragment].
 *  For a [KtCodeFragment], a source module context, a compiled library source context, or an empty context (`null`) are supported.
 *
 * @param options The compilation options created via [createCompilationOptions].
 *
 * @see createCompilationOptions
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.compilation' endpoint instead.",
    replaceWith = ReplaceWith(
        "compile(file, options)",
        "org.jetbrains.kotlin.analysis.api.compilation.compile",
    ),
)
@KaExperimentalApi
@Throws(KaCodeCompilationException::class)
@KaContextParameterApi
context(session: KaSession)
public fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult {
    return with(session) {
        compile(
            file = file,
            options = options,
        )
    }
}

/**
 * Creates a new [KaCompilationOptions] instance using the given DSL [init] block.
 *
 * Example usage:
 * ```kotlin
 * val options = createCompilationOptions {
 *     target(KaCompilationTarget.JVM)
 *     moduleName("myModule")
 *     languageVersionSettings(languageSettings)
 *     allowedErrorFilter { diagnostic -> diagnostic.factoryName in allowedErrors }
 * }
 * ```
 *
 * @param init A lambda that configures the [KaCompilationOptionsBuilder].
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.compilation' endpoint instead.",
    replaceWith = ReplaceWith(
        "createCompilationOptions(init)",
        "org.jetbrains.kotlin.analysis.api.compilation.createCompilationOptions",
    ),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions {
    return with(session) {
        createCompilationOptions(
            init = init,
        )
    }
}

/**
 * Creates a copy of these [KaCompilationOptions], applying the given [init] modifications.
 *
 * Options not explicitly overridden in [init] retain their original values.
 *
 * @param init A lambda that configures the [KaCompilationOptionsBuilder] with modifications to apply.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.compilation' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.modify(init)",
        "org.jetbrains.kotlin.analysis.api.compilation.modify",
    ),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaCompilationOptions.modify(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions {
    return with(session) {
        modify(
            init = init,
        )
    }
}
