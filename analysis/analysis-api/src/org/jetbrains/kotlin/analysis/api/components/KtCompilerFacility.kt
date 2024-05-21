/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
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
 * In-memory compilation result returned from [KaCompilerFacility].
 *
 * Compilation fails if there are critical errors reported either on the frontend or on the backend side.
 * Keep in mind that [KaCompilationResult] is a part of Analysis API, so it should only be used inside an analysis block.
 */
public sealed class KaCompilationResult {
    /**
     * Successful compilation result.
     *
     * @property output Output files produced by the compiler. For the JVM target, these are class files and '.kotlin_module'.
     * @property capturedValues Context values captured by a [KtCodeFragment]. Empty for an ordinary [KtFile].
     */
    public class Success(
        public val output: List<KaCompiledFile>,
        public val capturedValues: List<CodeFragmentCapturedValue>
    ) : KaCompilationResult()

    /**
     * Failed compilation result.
     *
     * @property errors Non-recoverable errors either during code analysis, or during code generation.
     */
    public class Failure(public val errors: List<KaDiagnostic>) : KaCompilationResult()
}

public typealias KtCompilationResult = KaCompilationResult

public interface KaCompiledFile {
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

public typealias KtCompiledFile = KaCompiledFile

/**
 * `true` if the compiled file is a Java class file.
 */
public val KaCompiledFile.isClassFile: Boolean
    get() = path.endsWith(".class", ignoreCase = true)

/**
 * Compilation target platform.
 */
public sealed class KaCompilerTarget {
    /** JVM target (produces '.class' files). */
    public class Jvm(public val classBuilderFactory: ClassBuilderFactory) : KaCompilerTarget()
}

public typealias KtCompilerTarget = KaCompilerTarget

public abstract class KaCompilerFacility : KaSessionComponent() {
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
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): KaCompilationResult
}

public typealias KtCompilerFacility = KaCompilerFacility

public interface KaCompilerFacilityMixIn : KaSessionMixIn {
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
     * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException].
     * The implementation should wrap the `compile()` call into a `try`/`catch` block when necessary.
     */
    @Throws(KaCodeCompilationException::class)
    public fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult {
        return withValidityAssertion {
            try {
                analysisSession.compilerFacility.compile(file, configuration, target, allowedErrorFilter)
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                throw KaCodeCompilationException(e)
            }
        }
    }
}

public typealias KtCompilerFacilityMixIn = KaCompilerFacilityMixIn

/**
 * Thrown when an exception occurred on analyzing the code to be compiled, or during target platform code generation.
 */
public class KaCodeCompilationException(cause: Throwable) : RuntimeException(cause)