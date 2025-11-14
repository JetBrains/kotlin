/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.serializeKlibHeader
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.validation.checkers.IrInlineDeclarationChecker
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.toKlibMetadataVersion
import java.io.File

/**
 * Holds the binary data for a single Kotlin file to be written to a KLIB, i.e., its metadata and IR (unless it's a metadata-only KLIB).
 *
 * @property metadata  Serialized metadata of the corresponding source file.
 * @property irData Serialized IR for this file, or `null` if this is a metadata-only KLIB.
 * @property path The path to the corresponding source file, or `null` if that source file didn't have a path.
 * @property fqName The fully qualified name of the package containing the serialized file.
 */
class KotlinFileSerializedData private constructor(
    val metadata: ByteArray,
    val irData: SerializedIrFile?,
    val path: String?,
    val fqName: String,
) {

    /**
     * Used for creating file serialization data for IR-containing KLIBs.
     *
     * @param metadata Serialized metadata of the corresponding source file.
     * @param irData Serialized IR for this file.
     */
    constructor(metadata: ByteArray, irData: SerializedIrFile) : this(metadata, irData, irData.path, irData.fqName)

    /**
     * Used for creating file serialization data in metadata-only KLIBs.
     *
     * @param metadata Serialized metadata of the corresponding source file.
     * @param path The path of the serialized file.
     * @param fqName The fully qualified name of the package containing the serialized file.
     */
    constructor(metadata: ByteArray, path: String?, fqName: String) : this(metadata, irData = null, path, fqName)
}

class SerializerOutput<Dependency : KotlinLibrary>(
    val serializedMetadata: SerializedMetadata?,
    val serializedIr: SerializedIrModule?,
    val neededLibraries: List<Dependency>,
)

fun KtSourceFile.toIoFileOrNull(): File? = when (this) {
    is KtIoFileSourceFile -> file
    is KtVirtualFileSourceFile -> VfsUtilCore.virtualToIoFile(virtualFile)
    is KtPsiSourceFile -> VfsUtilCore.virtualToIoFile(psiFile.virtualFile)
    else -> path?.let(::File)
}

/**
 * Produces all the necessary binary data for writing a KLIB for a Kotlin module, including its metadata.
 *
 * If [irModuleFragment] is not `null`, serializes the module's IR into binary form by running [IrModuleSerializer].
 *
 * For producing a metadata-only KLIB, pass `null` to [irModuleFragment].
 *
 * @param moduleName The name of the module being serialized to be written into the KLIB header.
 * @param irModuleFragment The IR to be serialized into the KLIB being produced, or `null` if this is going to be a metadata-only KLIB.
 * @param configuration Used to determine certain serialization parameters and enable/disable serialization diagnostics.
 * @param diagnosticReporter Used for reporting serialization-time diagnostics, for example, about clashing IR signatures.
 * @param cleanFiles In the case of incremental compilation, the list of files that were not changed and therefore don't need to be
 *     serialized again.
 * @param dependencies The list of KLIBs that the KLIB being produced depends on.
 * @param createModuleSerializer Used for creating a backend-specific instance of [IrModuleSerializer].
 * @param metadataSerializer Something capable of serializing the metadata of the source files. See the corresponding interface KDoc.
 * @param platformKlibCheckers Additional checks to be run before serializing [irModuleFragment].
 *     Can be used to report serialization-time diagnostics.
 * @param processCompiledFileData Called for each newly serialized file. Useful for incremental compilation.
 * @param processKlibHeader Called after serializing the KLIB header. Useful for incremental compilation.
 */
fun <Dependency : KotlinLibrary, SourceFile> serializeModuleIntoKlib(
    moduleName: String,
    irModuleFragment: IrModuleFragment?,
    configuration: CompilerConfiguration,
    diagnosticReporter: IrDiagnosticReporter,
    cleanFiles: List<KotlinFileSerializedData>,
    dependencies: List<Dependency>,
    createModuleSerializer: (irDiagnosticReporter: IrDiagnosticReporter) -> IrModuleSerializer<*>,
    metadataSerializer: KlibSingleFileMetadataSerializer<SourceFile>,
    platformKlibCheckers: List<(IrDiagnosticReporter) -> IrVisitor<*, Nothing?>> = emptyList(),
    processCompiledFileData: ((File, KotlinFileSerializedData) -> Unit)? = null,
    processKlibHeader: (ByteArray) -> Unit = {},
): SerializerOutput<Dependency> {
    val serializedIr = irModuleFragment?.let {
        it.runIrLevelCheckers(
            diagnosticReporter,
            *platformKlibCheckers.toTypedArray(),
        )

        if (!configuration.languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)) {
            // With IrIntraModuleInlinerBeforeKlibSerialization feature, this check happens after the first phase of KLIB inlining.
            // Without it, the check should happen here instead.
            it.runIrLevelCheckers(
                diagnosticReporter,
                ::IrInlineDeclarationChecker,
            )
        }

        createModuleSerializer(
            diagnosticReporter,
        ).serializedIrModule(it)
    }

    val serializedFiles = serializedIr?.files?.toList()

    val compiledKotlinFiles = buildList {
        addAll(cleanFiles)
        metadataSerializer.forEachFile { i, sourceFile, ktSourceFile, packageFqName ->
            val binaryFile = serializedFiles?.get(i)?.also {
                assert(ktSourceFile == null || ktSourceFile.path == it.path) {
                    """The Kt and Ir files are put in different order
                    Kt: ${ktSourceFile?.path}
                    Ir: ${it.path}
                    """.trimMargin()
                }
            }
            val protoBuf = metadataSerializer.serializeSingleFileMetadata(sourceFile)
            val metadata = protoBuf.toByteArray()
            val compiledKotlinFile = if (binaryFile == null)
                KotlinFileSerializedData(metadata, ktSourceFile?.path, packageFqName.asString())
            else
                KotlinFileSerializedData(metadata, binaryFile)

            if (processCompiledFileData != null) {
                val ioFile = ktSourceFile?.toIoFileOrNull() ?: error(
                    buildString {
                        appendLine("No file found for source ${ktSourceFile?.path}")
                        appendLine("This happened because there is a compiler plugin which generates new top-level declarations")
                        appendLine("and the incremental compilation is enabled.")
                        appendLine("Consider disabling the incremental compilation for this module or disable the plugin.")
                        appendLine("If you met this error, please describe your use-case in https://youtrack.jetbrains.com/issue/KT-82395")
                    }
                )
                processCompiledFileData(ioFile, compiledKotlinFile)
            }

            add(compiledKotlinFile)
        }
    }

    val header = serializeKlibHeader(
        languageVersionSettings = configuration.languageVersionSettings,
        moduleName = moduleName,
        fragmentNames = compiledKotlinFiles.map { it.fqName }.distinct().sorted(),
        emptyPackages = emptyList(),
    ).toByteArray()

    processKlibHeader(header)

    val (fragmentNames, fragmentParts) = compiledKotlinFiles
        .groupBy { it.fqName }
        .map { (fqn, data) ->
            fqn to data.sortedBy { it.path }.map { it.metadata }
        }
        .sortedBy { it.first }
        .unzip()

    val metadataVersion = configuration.languageVersionSettings.languageVersion.toKlibMetadataVersion().toArray()

    val serializedMetadata = SerializedMetadata(
        module = header,
        fragments = fragmentParts,
        fragmentNames = fragmentNames,
        metadataVersion = metadataVersion,
    )

    return SerializerOutput(
        serializedMetadata = serializedMetadata,
        serializedIr = if (serializedIr == null) null
        else SerializedIrModule(
            compiledKotlinFiles.mapNotNull { it.irData },
            serializedIr.fileWithPreparedInlinableFunctions,
        ),
        neededLibraries = dependencies,
    )
}

fun addLanguageFeaturesToManifest(manifestProperties: Properties, languageVersionSettings: LanguageVersionSettings) {
    val enabledFeatures = languageVersionSettings.getCustomizedEffectivelyEnabledLanguageFeatures()
    val presentableEnabledFeatures = enabledFeatures.sortedBy(LanguageFeature::name).joinToString(" ") { "+$it" }

    val disabledFeatures = languageVersionSettings.getCustomizedEffectivelyDisabledLanguageFeatures()
    val presentableDisabledFeatures = disabledFeatures.sortedBy(LanguageFeature::name).joinToString(" ") { "-$it" }

    val presentableAlteredFeatures = "$presentableEnabledFeatures $presentableDisabledFeatures".trim()
    if (presentableAlteredFeatures.isNotBlank()) {
        manifestProperties.setProperty(KLIB_PROPERTY_MANUALLY_ALTERED_LANGUAGE_FEATURES, presentableAlteredFeatures)
    }

    val presentablePoisoningFeatures =
        enabledFeatures.filter { it.forcesPreReleaseBinariesIfEnabled() }.sortedBy(LanguageFeature::name).joinToString(" ") { "+$it" }
    if (presentablePoisoningFeatures.isNotBlank()) {
        manifestProperties.setProperty(KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES, presentablePoisoningFeatures)
    }
}

private fun IrModuleFragment.runIrLevelCheckers(
    diagnosticReporter: IrDiagnosticReporter,
    vararg checkers: (IrDiagnosticReporter) -> IrVisitor<*, Nothing?>,
) {
    for (checker in checkers) {
        accept(checker(diagnosticReporter), null)
    }
}
