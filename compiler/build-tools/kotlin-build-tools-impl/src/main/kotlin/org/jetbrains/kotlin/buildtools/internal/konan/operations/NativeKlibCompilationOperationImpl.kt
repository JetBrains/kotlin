/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.konan.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.NativeArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import java.nio.file.Path

internal class NativeKlibCompilationOperationImpl private constructor(
    override val options: Options = Options(NativeKlibCompilationOperation::class),
    override val sources: List<Path>,
    override val destination: Path,
    compilerArguments: NativeArgumentsImpl = NativeArgumentsImpl(),
    private val compilerVersion: String,
) : BaseCompilationOperationImpl<NativeArgumentsImpl, K2NativeCompilerArguments>(compilerArguments),
    NativeKlibCompilationOperation, NativeKlibCompilationOperation.Builder,
    DeepCopyable<NativeKlibCompilationOperationImpl> {
    constructor(
        sources: List<Path>,
        destination: Path,
        compilerArguments: NativeArgumentsImpl = NativeArgumentsImpl(),
        compilerVersion: String,
    ) : this(
        options = Options(NativeKlibCompilationOperation::class),
        sources = sources,
        destination = destination,
        compilerArguments = compilerArguments,
        compilerVersion = compilerVersion,
    ) {
        initializeOptions(this::class, options)
    }

    override fun toBuilder(): NativeKlibCompilationOperation.Builder = deepCopy()

    override fun deepCopy(): NativeKlibCompilationOperationImpl {
        return NativeKlibCompilationOperationImpl(
            options.deepCopy(),
            sources,
            destination,
            compilerArguments.deepCopy(),
            compilerVersion
        )
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: NativeKlibCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: NativeKlibCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): NativeKlibCompilationOperation = deepCopy()

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun executeCancellableImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): CompilationResult {
        if (executionPolicy is DaemonExecutionPolicyImpl) {
            throw UnsupportedOperationException(DAEMON_NOT_SUPPORTED_MESSAGE)
        }
        checkKotlinNativeCompilerIsAvailable()
        return super.executeCancellableImpl(projectId, executionPolicy, logger, executionContext)
    }

    override fun getRootProjectDir(): Path? = null

    override fun createAndPrepareCompilerArguments(): K2NativeCompilerArguments =
        compilerArguments.toCompilerArguments().also { compilerArguments ->
            compilerArguments.produce = "library"
            compilerArguments.outputName = destination.absolutePathStringOrThrow()
        }

    // There is no Kotlin/Native entry in the daemon protocol yet, see executeCancellableImpl
    override val targetPlatform: CompileService.TargetPlatform
        get() = throw UnsupportedOperationException(DAEMON_NOT_SUPPORTED_MESSAGE)

    override fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: K2NativeCompilerArguments,
    ): IncrementalCompilationOptions? = null

    override fun shouldCompileIncrementally(): Boolean = false

    override fun createCompiler(): CLICompiler<K2NativeCompilerArguments> = K2Native()

    override fun K2NativeCompilerArguments.addSources() {
        freeArgs += sources.map { it.absolutePathStringOrThrow() }
    }

    override fun compileIncrementallyInProcess(
        arguments: K2NativeCompilerArguments,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        executionContext: ExecutionContext,
    ): CompilationResult {
        error("Kotlin/Native klib compilation doesn't support incremental compilation")
    }

    companion object {
        private const val DAEMON_NOT_SUPPORTED_MESSAGE =
            "Kotlin/Native compilation via the Kotlin daemon is not supported yet. Use the in-process execution policy."

        private const val K2NATIVE_CLASS_NAME = "org.jetbrains.kotlin.cli.bc.K2Native"

        /**
         * The Kotlin/Native compiler is not a dependency of the Build Tools API implementation (in particular, it is absent when the
         * implementation is built with Kotlin/Native disabled), so its presence is checked explicitly to report a clear reason
         * instead of a [NoClassDefFoundError] from the middle of the compilation.
         */
        private fun checkKotlinNativeCompilerIsAvailable() {
            try {
                Class.forName(K2NATIVE_CLASS_NAME, false, NativeKlibCompilationOperationImpl::class.java.classLoader)
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException(
                    "The Kotlin/Native compiler is not available: class $K2NATIVE_CLASS_NAME is not on the classpath of the Kotlin Build Tools API implementation. " +
                            "Rebuild BTA with Kotlin/Native enabled.",
                    e
                )
            }
        }
    }
}
