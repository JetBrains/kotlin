/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import java.io.File

/**
 * An in-memory compilation result returned from [KaCompilerFacility].
 *
 * Compilation fails if there are critical errors reported either on the frontend or on the backend side.
 * Keep in mind that [KaCompilationResult] is a part of the Analysis API, so it should only be used inside an
 * [analysis block][org.jetbrains.kotlin.analysis.api.analyze].
 */
@KaExperimentalApi
public sealed class KaCompilationResult {
    /**
     * A successful compilation result.
     *
     * @property output Output files produced by the compiler. For the JVM target, these are class files and '.kotlin_module'.
     * @property capturedValues Context values captured by a [KtCodeFragment]. Empty for an ordinary [KtFile].
     */
    @KaExperimentalApi
    public class Success(
        public val output: List<KaCompiledFile>,
        public val capturedValues: List<CodeFragmentCapturedValue>
    ) : KaCompilationResult()

    /**
     * A failed compilation result.
     *
     * @property errors Non-recoverable errors which occurred either during code analysis or during code generation.
     */
    @KaExperimentalApi
    public class Failure(public val errors: List<KaDiagnostic>) : KaCompilationResult()
}

@KaExperimentalApi
public interface KaCompiledFile {
    /**
     * The path of the compiled file relative to the root of the output directory.
     */
    public val path: String

    /**
     * The source files that were compiled to produce this file.
     */
    public val sourceFiles: List<File>

    /**
     * The content of the compiled file.
     */
    public val content: ByteArray
}

/**
 * Whether the compiled file is a Java class file.
 */
@KaExperimentalApi
public val KaCompiledFile.isClassFile: Boolean
    get() = path.endsWith(".class", ignoreCase = true)

/**
 * The target platform of the compilation performed by [KaCompilerFacility].
 */
@KaExperimentalApi
public sealed class KaCompilerTarget {
    /**
     * JVM target (produces '.class' files).
     *
     * @property isTestMode `true` if the underlying code should support dumping the bytecode of the resulting class files to text.
     * @property compiledClassHandler A handler which is called whenever a new class file is produced.
     */
    @KaExperimentalApi
    public class Jvm(
        public val isTestMode: Boolean,
        public val compiledClassHandler: KaCompiledClassHandler? = null,
    ) : KaCompilerTarget()
}

/**
 * A handler which is called whenever a new class file is produced, when compiling sources to the JVM target.
 *
 * @see KaCompilerTarget.Jvm
 */
@KaExperimentalApi
public fun interface KaCompiledClassHandler {
    /**
     * [handleClassDefinition] is called whenever a new class file is produced.
     *
     * @param file The [PsiFile] containing the class definition. It can be `null` when the generated class file has no PSI file in sources,
     *  for example if it's an anonymous object from another module, regenerated during inlining.
     * @param className The name of the class in the JVM's internal name format, for example `"java/lang/Object"`.
     */
    public fun handleClassDefinition(file: PsiFile?, className: String)
}

@KaExperimentalApi
public interface KaCompilerFacility : KaSessionComponent {
    @KaExperimentalApi
    public companion object {
        /** Simple class name for the code fragment facade class. */
        public val CODE_FRAGMENT_CLASS_NAME: CompilerConfigurationKey<String> =
            CompilerConfigurationKey<String>("code fragment class name")

        /** Entry point method name for the code fragment. */
        public val CODE_FRAGMENT_METHOD_NAME: CompilerConfigurationKey<String> =
            CompilerConfigurationKey<String>("code fragment method name")
    }

    /**
     * Compiles the given [file] in-memory (without dumping the compiled binaries to the disk).
     *
     * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException]. The implementation should wrap the
     * `compile()` call into a `try`/`catch` block when necessary.
     *
     * @param file A file to compile.
     *  The file must be either a source module file, or a [KtCodeFragment].
     *  For a [KtCodeFragment], a source module context, a compiled library source context, or an empty context(`null`) are supported.
     *
     * @param configuration The compiler configuration.
     *  It is recommended to submit at least the module name ([CommonConfigurationKeys.MODULE_NAME])
     *  and language version settings ([CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS]).
     *
     * @param target The target platform of the compilation.
     *
     * @param allowedErrorFilter A filter for allowed errors. Compilation will be aborted if there are errors that this filter rejects.
     */
    @KaExperimentalApi
    @Throws(KaCodeCompilationException::class)
    public fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult
}

/**
 * Thrown when an exception occurred while analyzing the code to be compiled, or during target platform code generation.
 *
 * @see KaCompilerFacility
 */
@KaExperimentalApi
public class KaCodeCompilationException(cause: Throwable) : RuntimeException(cause)
