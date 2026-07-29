/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.js.tsexport.TypeScriptExportConfig
import org.jetbrains.kotlin.js.tsexport.TypeScriptModuleConfig
import org.jetbrains.kotlin.js.tsexport.runTypeScriptExport
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import java.nio.file.Path

internal class JsDtsGenerationOperationImpl private constructor(
    override val options: Options,
    override val klibs: List<Path>,
    override val outputDirectory: Path,
) : BuildOperationImpl<CompilationResult>(), JsDtsGenerationOperation, JsDtsGenerationOperation.Builder,
    DeepCopyable<JsDtsGenerationOperationImpl> {

    constructor(klibs: List<Path>, outputDirectory: Path) : this(
        options = Options(JsDtsGenerationOperation::class),
        klibs = klibs,
        outputDirectory = outputDirectory,
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        // implementation goes here
        val typeScriptExportConfig: TypeScriptExportConfig = TODO("Not yet implemented")
        runTypeScriptExport(transformKlibsIntoKlibInputModule(klibs), typeScriptExportConfig)
        return CompilationResult.COMPILATION_SUCCESS
    }

    private fun transformKlibsIntoKlibInputModule(klibs: List<Path>): List<KlibInputModule<TypeScriptModuleConfig>> =
        klibs.map {
            val name = TODO("Not yet implemented") // read from the klib manifest?
            val outputName = TODO("Not yet implemented")
            KlibInputModule(name, it, TypeScriptModuleConfig(outputName))
        }

    override fun configureFrom(linkingOperation: JsLinkingOperation) {
        check(linkingOperation is JsLinkingOperationImpl) { "Unexpected linking operation: ${linkingOperation::class}." }
        this[MODULE_KIND] = linkingOperation.compilerArguments[JsArgumentsImpl.MODULE_KIND] ?: JsModuleKind.PLAIN // todo: should it be a fallback to PLAIN?
        this[COMPILE_LONG_AS_BIG_INT] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ES_LONG_AS_BIGINT] ?: false // todo should it be a fallback to false?
        this[IMPLEMENT_INTERFACES] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT]
        this[EXPORT_SUSPEND_LAMBDAS] = linkingOperation.compilerArguments[JsArgumentsImpl.X_SUSPEND_LAMBDA_EXPORTING]
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsDtsGenerationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsDtsGenerationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun toBuilder(): JsDtsGenerationOperation.Builder = deepCopy()

    override fun build(): JsDtsGenerationOperation = deepCopy()

    override fun deepCopy(): JsDtsGenerationOperationImpl =
        JsDtsGenerationOperationImpl(options.deepCopy(), klibs, outputDirectory)

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        private val defaultArgsReference = K2JSCompilerArguments()
        val TS_COMPILATION_STRATEGY: Option<JsDtsCompilationStrategy> = Option("TS_COMPILATION_STRATEGY", JsDtsCompilationStrategy.MERGED)
        val GRANULARITY: Option<JsDtsGranularity> = Option("GRANULARITY", JsDtsGranularity.WHOLE_PROGRAM)
        val MODULE_KIND: Option<JsModuleKind> = Option(
            "MODULE_KIND",
            defaultArgsReference.moduleKind
                ?.let {
                    JsModuleKind.entries.firstOrNull { entry ->
                        entry.stringValue.equals(
                            it,
                            false
                        )
                    } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it")
                } ?: JsModuleKind.PLAIN // todo: should it be a fallback to PLAIN?
        )
        val COMPILE_LONG_AS_BIG_INT: Option<Boolean> = Option("COMPILE_LONG_AS_BIG_INT", defaultArgsReference.compileLongAsBigInt ?: false) // todo should it be a fallback to false?
        val IMPLEMENT_INTERFACES: Option<Boolean> = Option("IMPLEMENT_INTERFACES", defaultArgsReference.allowImplementableInterfacesExporting)
        val EXPORT_SUSPEND_LAMBDAS: Option<Boolean> = Option("EXPORT_SUSPEND_LAMBDAS", defaultArgsReference.allowExportingSuspendLambdas)
    }
}
