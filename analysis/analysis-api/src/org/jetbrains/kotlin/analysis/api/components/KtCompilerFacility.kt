/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtCodeFragment
import java.io.File

/**
 * In-memory compilation result returned from [KtCompilerFacility].
 *
 * Compilation fails if there are critical errors reported either on the frontend or on the backend side.
 * Keep in mind that [KtCompilationResult] is a part of Analysis API, so it should only be used inside an analysis block.
 */
public sealed class KtCompilationResult {
    /**
     * Successful compilation result.
     *
     * @property output Output files produced by the compiler. For the JVM target, these are class files and '.kotlin_module'.
     * @property capturedValues Context values captured by a [KtCodeFragment]. Empty for an ordinary [KtFile].
     */
    public class Success(
        public val output: List<KtCompiledFile>,
        public val capturedValues: List<CodeFragmentCapturedValue>
    ) : KtCompilationResult()

    /**
     * Failed compilation result.
     *
     * @property errors Non-recoverable errors either during code analysis, or during code generation.
     */
    public class Failure(public val errors: List<KtDiagnostic>) : KtCompilationResult()
}

public interface KtCompiledFile {
    /**
     * Path of the compiled file relative to the root of the output directory.
     */
    public val path: String

    /**
     * Source files that were compiled to produce this file.
     */
    public val sourceFiles: List<File>

    /**
     * Content of the compiled file.
     */
    public val content: ByteArray
}

/**
 * `true` if the compiled file is a Java class file.
 */
public val KtCompiledFile.isClassFile: Boolean
    get() = path.endsWith(".class", ignoreCase = true)

/**
 * Compilation target platform.
 */
public sealed class KtCompilerTarget {
    /** JVM target (produces '.class' files). */
    public class Jvm(public val classBuilderFactory: ClassBuilderFactory) : KtCompilerTarget()
}

public abstract class KtCompilerFacility : KtAnalysisSessionComponent() {
    public companion object {
        /** Simple class name for the code fragment facade class. */
        public val CODE_FRAGMENT_CLASS_NAME: CompilerConfigurationKey<String> =
            CompilerConfigurationKey<String>("code fragment class name")

        /** Entry point method name for the code fragment. */
        public val CODE_FRAGMENT_METHOD_NAME: CompilerConfigurationKey<String> =
            CompilerConfigurationKey<String>("code fragment method name")
    }

    public abstract fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilerTarget,
        allowedErrorFilter: (KtDiagnostic) -> Boolean,
    ): KtCompilationResult
}

public interface KtCompilerFacilityMixIn : KtAnalysisSessionMixIn {
    /**
     * Compile the given [file] in-memory (without dumping the compiled binaries to a disk).
     *
     * @param file A file to compile.
     *  The file must be either a source module file, or a [KtCodeFragment].
     *  For a [KtCodeFragment], a source module context, a compiled library source context, or an empty context(`null`) are supported.
     *
     * @param configuration Compiler configuration.
     *  It is recommended to submit at least the module name ([CommonConfigurationKeys.MODULE_NAME])
     *  and language version settings ([CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]).
     *
     * @param target Compilation target platform.
     *
     * @param allowedErrorFilter Filter for the allowed errors.
     * Compilation will be aborted if there are errors that this filter rejects.
     *
     * @return Compilation result.
     *
     * The function rethrows exceptions from the compiler, wrapped in [KtCodeCompilationException].
     * The implementation should wrap the `compile()` call into a `try`/`catch` block when necessary.
     */
    @Throws(KtCodeCompilationException::class)
    public fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilerTarget,
        allowedErrorFilter: (KtDiagnostic) -> Boolean
    ): KtCompilationResult {
        return withValidityAssertion {
            try {
                analysisSession.compilerFacility.compile(file, configuration, target, allowedErrorFilter)
            } catch (e: Throwable) {
                throw KtCodeCompilationException(e)
            }
        }
    }
}

/**
 * Thrown when an exception occurred on analyzing the code to be compiled, or during target platform code generation.
 */
public class KtCodeCompilationException(cause: Throwable) : RuntimeException(cause)