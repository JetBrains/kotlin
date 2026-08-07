/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.tsexport.TypeScriptExportConfig
import org.jetbrains.kotlin.js.tsexport.TypeScriptModuleConfig
import org.jetbrains.kotlin.js.tsexport.createTypeScriptExportInputModule
import org.jetbrains.kotlin.js.tsexport.runTypeScriptExport
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.platform.js.JsPlatforms
import java.io.File
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

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        sessionIsAliveFlagFile: Lazy<File>,
    ): CompilationResult {
        val inputModules = transformKlibsIntoKlibInputModule(klibs, logger)
        // The main KLIB is the last one in the list; its manifest drives the merged artifact naming.
        val mainModule = inputModules.last()
        val typeScriptExportConfig = TypeScriptExportConfig(
            targetPlatform = JsPlatforms.defaultJsPlatform,
            artifactConfiguration = WebArtifactConfiguration(
                moduleKind = ModuleKind.fromType(this[MODULE_KIND].stringValue),
                moduleName = mainModule.name,
                outputDirectory = outputDirectory.toFile(),
                outputName = mainModule.config.outputName ?: mainModule.name,
                granularity = JsGenerationGranularity.valueOf(this[GRANULARITY].name),
                tsCompilationStrategy = TsCompilationStrategy.valueOf(this[TS_COMPILATION_STRATEGY].name),
                production = false,
                minimizedMemberNames = false,
            ),
            compileLongAsBigInt = this[COMPILE_LONG_AS_BIG_INT],
            implementableInterfaces = this[IMPLEMENT_INTERFACES],
            exportableSuspendLambdas = this[EXPORT_SUSPEND_LAMBDAS],
            dataClassCopyRespectsConstructorVisibility = this[DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY],
            exportUntypedAsUnknown = this[EXPORT_UNTYPED_AS_UNKNOWN],
        )
        runTypeScriptExport(inputModules, typeScriptExportConfig)
        return CompilationResult.COMPILATION_SUCCESS
    }

    override val usesApplicationEnvironment: Boolean
        get() = true

    private fun transformKlibsIntoKlibInputModule(
        klibs: List<Path>,
        logger: KotlinLogger?,
    ): List<KlibInputModule<TypeScriptModuleConfig>> =
        klibs.map { path ->
            createTypeScriptExportInputModule(path) { _, message ->
                logger?.error(message)
                error(message)
            }
        }

    override fun configureFrom(linkingOperation: JsLinkingOperation) {
        check(linkingOperation is JsLinkingOperationImpl) { "Unexpected linking operation: ${linkingOperation::class}." }

        this[COMPILE_LONG_AS_BIG_INT] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ES_LONG_AS_BIGINT] ?: false
        this[IMPLEMENT_INTERFACES] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT]
        this[EXPORT_SUSPEND_LAMBDAS] = linkingOperation.compilerArguments[JsArgumentsImpl.X_SUSPEND_LAMBDA_EXPORTING]
        this[DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY] =
            linkingOperation.compilerArguments[CommonCompilerArgumentsImpl.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY]
        this[EXPORT_UNTYPED_AS_UNKNOWN] =
            linkingOperation.compilerArguments[JsArgumentsImpl.X_TS_EXPORT_UNTYPED_AS_UNKNOWN]

        this[MODULE_KIND] = linkingOperation.compilerArguments[JsArgumentsImpl.MODULE_KIND]
            ?: JsModuleKind.ES.takeIf {
                val target = linkingOperation.compilerArguments[JsArgumentsImpl.TARGET]
                target != null && target >= JsEcmaVersion.ES2015
            }
            ?: JsModuleKind.UMD

        this[GRANULARITY] = when {
            linkingOperation.compilerArguments[JsArgumentsImpl.X_IR_PER_FILE] -> JsDtsGranularity.PER_FILE
            // Right now, we don't support per-module d.ts generation for IR. So for the backward compatibility, we use whole-program granularity.
            linkingOperation.compilerArguments[JsArgumentsImpl.X_IR_PER_MODULE] -> JsDtsGranularity.WHOLE_PROGRAM
            else -> JsDtsGranularity.WHOLE_PROGRAM
        }
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
                } ?: JsModuleKind.UMD
        )
        val COMPILE_LONG_AS_BIG_INT: Option<Boolean> = Option("COMPILE_LONG_AS_BIG_INT", defaultArgsReference.compileLongAsBigInt ?: false)
        val IMPLEMENT_INTERFACES: Option<Boolean> = Option("IMPLEMENT_INTERFACES", defaultArgsReference.allowImplementableInterfacesExporting)
        val EXPORT_SUSPEND_LAMBDAS: Option<Boolean> = Option("EXPORT_SUSPEND_LAMBDAS", defaultArgsReference.allowExportingSuspendLambdas)
        val EXPORT_UNTYPED_AS_UNKNOWN: Option<Boolean> = Option("EXPORT_UNTYPED_AS_UNKNOWN", defaultArgsReference.exportUntypedAsUnknown)
        val DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY: Option<Boolean> =
            Option("DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY", defaultArgsReference.consistentDataClassCopyVisibility)
    }
}
