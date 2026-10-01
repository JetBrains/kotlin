/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentValueAdapter
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.tsexport.TypeScriptExportConfig
import org.jetbrains.kotlin.js.tsexport.TypeScriptModuleConfig
import org.jetbrains.kotlin.js.tsexport.runTypeScriptExport
import org.jetbrains.kotlin.library.jsOutputName
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.platform.js.JsPlatforms
import java.nio.file.Path

@Serializable
internal class JsDtsGenerationOperationImpl(
    override val klibs: List<Path>,
    override val outputDirectory: Path,
    @SerialName("TS_COMPILATION_STRATEGY") internal var tsCompilationStrategy: JsDtsCompilationStrategy = MERGED,
    @SerialName("GRANULARITY") internal var granularity: JsDtsGranularity = WHOLE_PROGRAM,
    @SerialName("MODULE_KIND") internal var moduleKind: JsModuleKind = defaultArgsReference.moduleKind
        ?.let {
            JsModuleKind.values().firstOrNull { entry ->
                entry.stringValue.equals(
                    it,
                    false
                )
            } ?: throw CompilerArgumentsParseException("Unknown -module-kind value: $it")
        } ?: UMD,
    @SerialName("COMPILE_LONG_AS_BIG_INT") internal var compileLongAsBigInt: Boolean = defaultArgsReference.compileLongAsBigInt ?: false,
    @SerialName("IMPLEMENT_INTERFACES") internal var implementInterfaces: Boolean = defaultArgsReference.allowImplementableInterfacesExporting,
    @SerialName("EXPORT_SUSPEND_LAMBDAS") internal var exportSuspendLambdas: Boolean = defaultArgsReference.allowExportingSuspendLambdas,
    @SerialName("USE_UNKNOWN_INSTEAD_ANY") internal var useUnknownInsteadAny: Boolean = defaultArgsReference.useUnknownInsteadAny,
    @SerialName("DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY") internal var dataClassCopyRespectsConstructorVisibility: Boolean = defaultArgsReference.consistentDataClassCopyVisibility,
) : BuildOperationImpl<CompilationResult>(), JsDtsGenerationOperation, JsDtsGenerationOperation.Builder,
    DeepCopyable<JsDtsGenerationOperationImpl> {

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): CompilationResult {
        val inputModules = transformKlibsIntoKlibInputModule(klibs)
        // The main KLIB is the last one in the list; its manifest drives the merged artifact naming.
        // If there is no klib provided, there is nothing to generate
        val mainModule = inputModules.lastOrNull() ?: return CompilationResult.COMPILATION_SUCCESS
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
            useUnknownInsteadAny = this[USE_UNKNOWN_INSTEAD_ANY],
        )
        runTypeScriptExport(inputModules, typeScriptExportConfig)
        return CompilationResult.COMPILATION_SUCCESS
    }

    override val usesApplicationEnvironment: Boolean
        get() = true

    private fun transformKlibsIntoKlibInputModule(klibs: List<Path>): List<KlibInputModule<TypeScriptModuleConfig>> =
        KlibLoader { libraryPaths(klibs.map(Path::toString)) }
            .load()
            .librariesStdlibFirst
            .map { KlibInputModule(it.uniqueName, it.path, TypeScriptModuleConfig(outputName = it.jsOutputName)) }

    @Suppress("EnumValuesSoftDeprecate")
    override fun configureFrom(linkingOperation: JsLinkingOperation) {
        check(linkingOperation is JsLinkingOperationImpl) { "Unexpected linking operation: ${linkingOperation::class}." }

        this[COMPILE_LONG_AS_BIG_INT] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ES_LONG_AS_BIGINT] ?: false
        this[IMPLEMENT_INTERFACES] = linkingOperation.compilerArguments[JsArgumentsImpl.X_ENABLE_IMPLEMENTING_INTERFACES_FROM_TYPESCRIPT]
        this[EXPORT_SUSPEND_LAMBDAS] = linkingOperation.compilerArguments[JsArgumentsImpl.X_SUSPEND_LAMBDA_EXPORTING]
        this[DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY] =
            linkingOperation.compilerArguments[CommonCompilerArgumentsImpl.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY]
        this[USE_UNKNOWN_INSTEAD_ANY] =
            linkingOperation.compilerArguments[JsArgumentsImpl.X_DTS_USE_UNKNOWN_INSTEAD_ANY]

        this[MODULE_KIND] = linkingOperation.compilerArguments[JsArgumentsImpl.MODULE_KIND]
            ?: JsModuleKind.ES.takeIf {
                val target = linkingOperation.compilerArguments[JsArgumentsImpl.TARGET]
                    ?.let { from -> JsEcmaVersion.values().first { it.name == from.name } }
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
    override fun <V> get(key: JsDtsGenerationOperation.Option<V>): V = JsArgumentValueAdapter.toApi(
        JsDtsGenerationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)
    )

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsDtsGenerationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        JsDtsGenerationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, JsArgumentValueAdapter.toImpl(value))
    }

    private operator fun <V> get(key: Option<V>): V =
        JsDtsGenerationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    private operator fun <V> set(key: Option<V>, value: V) {
        JsDtsGenerationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun toBuilder(): JsDtsGenerationOperation.Builder = deepCopy()

    override fun build(): JsDtsGenerationOperation = deepCopy()

    override fun deepCopy(): JsDtsGenerationOperationImpl =
        JsDtsGenerationOperationImpl(
            klibs = klibs,
            outputDirectory = outputDirectory,
            tsCompilationStrategy = tsCompilationStrategy,
            granularity = granularity,
            moduleKind = moduleKind,
            compileLongAsBigInt = compileLongAsBigInt,
            implementInterfaces = implementInterfaces,
            exportSuspendLambdas = exportSuspendLambdas,
            useUnknownInsteadAny = useUnknownInsteadAny,
            dataClassCopyRespectsConstructorVisibility = dataClassCopyRespectsConstructorVisibility
        ).also {
            it.copyFrom(this)
        }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        private val defaultArgsReference = K2JSCompilerArguments()
        val TS_COMPILATION_STRATEGY: Option<JsDtsCompilationStrategy> = Option("TS_COMPILATION_STRATEGY")
        val GRANULARITY: Option<JsDtsGranularity> = Option("GRANULARITY")
        val MODULE_KIND: Option<JsModuleKind> = Option("MODULE_KIND")
        val COMPILE_LONG_AS_BIG_INT: Option<Boolean> = Option("COMPILE_LONG_AS_BIG_INT")
        val IMPLEMENT_INTERFACES: Option<Boolean> = Option("IMPLEMENT_INTERFACES")
        val EXPORT_SUSPEND_LAMBDAS: Option<Boolean> = Option("EXPORT_SUSPEND_LAMBDAS")
        val USE_UNKNOWN_INSTEAD_ANY: Option<Boolean> = Option("USE_UNKNOWN_INSTEAD_ANY")
        val DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY: Option<Boolean> =
            Option("DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY")
    }
}
