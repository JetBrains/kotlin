/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.internal.js.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.arguments.JsArgumentValueAdapter
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.internal.serializability.CompilationResultSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.SupportsNewDaemonMode
import org.jetbrains.kotlin.buildtools.internal.serializability.deserialize
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
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
    @SerialName("EXPORT_UNTYPED_AS_UNKNOWN") internal var exportUntypedAsUnknown: Boolean = defaultArgsReference.useUnknownInsteadAny,
    @SerialName("DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY") internal var dataClassCopyRespectsConstructorVisibility: Boolean = defaultArgsReference.consistentDataClassCopyVisibility,
) : BuildOperationImpl<CompilationResult>(), JsDtsGenerationOperation, JsDtsGenerationOperation.Builder,
    DeepCopyable<JsDtsGenerationOperationImpl>, SupportsNewDaemonMode<CompilationResult> {

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
                moduleKind = ModuleKind.fromType(moduleKind.stringValue),
                moduleName = mainModule.name,
                outputDirectory = outputDirectory.toFile(),
                outputName = mainModule.config.outputName ?: mainModule.name,
                granularity = JsGenerationGranularity.valueOf(granularity.name),
                tsCompilationStrategy = TsCompilationStrategy.valueOf(tsCompilationStrategy.name),
                production = false,
                minimizedMemberNames = false,
            ),
            compileLongAsBigInt = compileLongAsBigInt,
            implementableInterfaces = implementInterfaces,
            exportableSuspendLambdas = exportSuspendLambdas,
            dataClassCopyRespectsConstructorVisibility = dataClassCopyRespectsConstructorVisibility,
            useUnknownInsteadAny = exportUntypedAsUnknown,
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

        compileLongAsBigInt = linkingOperation.compilerArguments.compileLongAsBigInt ?: false
        implementInterfaces = linkingOperation.compilerArguments.allowImplementableInterfacesExporting
        exportSuspendLambdas = linkingOperation.compilerArguments.allowExportingSuspendLambdas
        dataClassCopyRespectsConstructorVisibility =
            linkingOperation.compilerArguments.consistentDataClassCopyVisibility
        exportUntypedAsUnknown =
            linkingOperation.compilerArguments.useUnknownInsteadAny

        moduleKind = linkingOperation.compilerArguments.moduleKind
            ?: JsModuleKind.ES.takeIf {
                val target = linkingOperation.compilerArguments.target
                    ?.let { from -> JsEcmaVersion.values().first { it.name == from.name } }
                target != null && target >= JsEcmaVersion.ES2015
            }
                    ?: JsModuleKind.UMD

        granularity = when {
            linkingOperation.compilerArguments.irPerFile -> JsDtsGranularity.PER_FILE
            // Right now, we don't support per-module d.ts generation for IR. So for the backward compatibility, we use whole-program granularity.
            linkingOperation.compilerArguments.irPerModule -> JsDtsGranularity.WHOLE_PROGRAM
            else -> JsDtsGranularity.WHOLE_PROGRAM
        }
    }

    @UseFromImplModuleRestricted
    @Suppress("UNCHECKED_CAST")
    override fun <V> get(key: JsDtsGenerationOperation.Option<V>): V =
        JsArgumentValueAdapter.toApi(getPropertyByName(propertyAccessMap, key.id)) as V


    @UseFromImplModuleRestricted
    override fun <V> set(key: JsDtsGenerationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyAccessMap, key.id, JsArgumentValueAdapter.toImpl(value))
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
            exportUntypedAsUnknown = exportUntypedAsUnknown,
            dataClassCopyRespectsConstructorVisibility = dataClassCopyRespectsConstructorVisibility
        ).also {
            it.copyFrom(this)
        }

    override fun deserializeResult(serializedData: SerializedDataContainer): CompilationResult {
        return KotlinToolchainsImpl.serializer.deserialize(CompilationResultSerializer, serializedData)
    }

    override fun serializeResult(result: CompilationResult): SerializedDataContainer {
        return KotlinToolchainsImpl.serializer.serialize(CompilationResultSerializer, result)
    }

    companion object {
        private val defaultArgsReference = K2JSCompilerArguments()
        private val propertyAccessMap = mapOf(
            "TS_COMPILATION_STRATEGY" to JsDtsGenerationOperationImpl::tsCompilationStrategy,
            "GRANULARITY" to JsDtsGenerationOperationImpl::granularity,
            "MODULE_KIND" to JsDtsGenerationOperationImpl::moduleKind,
            "COMPILE_LONG_AS_BIG_INT" to JsDtsGenerationOperationImpl::compileLongAsBigInt,
            "IMPLEMENT_INTERFACES" to JsDtsGenerationOperationImpl::implementInterfaces,
            "EXPORT_SUSPEND_LAMBDAS" to JsDtsGenerationOperationImpl::exportSuspendLambdas,
            "EXPORT_UNTYPED_AS_UNKNOWN" to JsDtsGenerationOperationImpl::exportUntypedAsUnknown,
            "DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY" to JsDtsGenerationOperationImpl::dataClassCopyRespectsConstructorVisibility
        )
    }
}
