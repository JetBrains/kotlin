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
import org.jetbrains.kotlin.backend.common.deserializeSignaturesIndex
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serializeSignaturesIndex
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.addMetadataFlagsToHeader
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.util.toMetadataVersion
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.util.*

/**
 * Holds the binary data for a single Kotlin file to be written to a KLIB, i.e., its metadata and IR (unless it's a metadata-only KLIB).
 *
 * @property metadata  Serialized metadata of the corresponding source file.
 * @property irData Serialized IR for this file, or `null` if this is a metadata-only KLIB.
 * @property irInlineData Serialized IR for preprocessed inline functions.
 * It is `null` if this is a metadata-only KLIB or there were no inline functions in this file.
 * @property signatureIndex Serialized value of [SerializedSignatureIndex].
 * @property path The path to the corresponding source file, or `null` if that source file didn't have a path.
 * @property fqName The fully qualified name of the package containing the serialized file.
 */
class KotlinFileSerializedData private constructor(
    val metadata: ByteArray,
    val irData: SerializedIrFile?,
    val irInlineData: SerializedIrFile?,
    val signatureIndex: ByteArray?,
    val path: String?,
    val fqName: String,
) {

    /**
     * Used for creating file serialization data for IR-containing KLIBs.
     *
     * @param metadata Serialized metadata of the corresponding source file.
     * @param irData Serialized IR for this file.
     * @param irInlineData Serialized IR for preprocessed inline functions that were present in this file.
     * @param signatureIndex Serialized value of [SerializedSignatureIndex]
     */
    constructor(
        metadata: ByteArray, irData: SerializedIrFile, irInlineData: SerializedIrFile?, signatureIndex: ByteArray?
    ) : this(
        metadata, irData, irInlineData, signatureIndex, irData.path, irData.fqName
    ) {
        if (irInlineData != null) {
            require(irData.path == irInlineData.path && irData.fqName == irInlineData.fqName)
        }
    }

    /**
     * Used for creating file serialization data in metadata-only KLIBs.
     *
     * @param metadata Serialized metadata of the corresponding source file.
     * @param path The path of the serialized file.
     * @param fqName The fully qualified name of the package containing the serialized file.
     */
    constructor(metadata: ByteArray, path: String?, fqName: String) : this(
        metadata, irData = null, irInlineData = null, signatureIndex = null, path, fqName
    )
}

class SerializerOutput(
    val serializedMetadata: SerializedMetadata,
    val serializedIr: SerializedIrModule?,
    val neededLibraries: Collection<KotlinLibrary>,
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
 * @param processCompiledFileData Called for each newly serialized file. Useful for incremental compilation.
 */
fun <SourceFile> serializeModuleIntoKlib(
    moduleName: String,
    irModuleFragment: IrModuleFragment?,
    configuration: CompilerConfiguration,
    diagnosticReporter: IrDiagnosticReporter,
    cleanFiles: List<KotlinFileSerializedData>,
    dependencies: List<KotlinLibrary>,
    createModuleSerializer: (irDiagnosticReporter: IrDiagnosticReporter) -> IrModuleSerializer<*>,
    metadataSerializer: KlibSingleFileMetadataSerializer<SourceFile>,
    processCompiledFileData: ((File, KotlinFileSerializedData, List<CallableId>) -> Unit)? = null,
): SerializerOutput {
    val serializedIrFromDirtySources = irModuleFragment?.let {
        createModuleSerializer(
            diagnosticReporter,
        ).serializedIrModule(it)
    }

    val globalImportedSignatures: MutableSet<IdSignature> =
        serializedIrFromDirtySources.orEmpty().flatMapTo(mutableSetOf()) { it.importedSignatures }
    val globalExportedSignatures: MutableSet<IdSignature> =
        serializedIrFromDirtySources.orEmpty().flatMapTo(mutableSetOf()) { it.exportedSignatures }

    val compiledKotlinFiles = buildList {
        addAll(cleanFiles)
        metadataSerializer.forEachFile { i, ioFile, sourceFile, ktSourceFile, packageFqName ->
            val protoBuf = metadataSerializer.serializeSingleFileMetadata(sourceFile)
            val metadata = protoBuf.toByteArray()

            val serializedIr = serializedIrFromDirtySources?.get(i)
            val compiledKotlinFile = if (serializedIr == null) {
                KotlinFileSerializedData(metadata, ktSourceFile?.path, packageFqName.asString())
            } else {
                require(ktSourceFile == null || ktSourceFile.path == serializedIr.irData.path) {
                    """The Kt and Ir files are put in different order
                    Kt: ${ktSourceFile?.path}
                    Ir: ${serializedIr.irData.path}
                    """.trimMargin()
                }

                val serializedSignatureIndex = serializeSignaturesIndex(
                    exportedSignatures = serializedIr.exportedSignatures,
                    importedSignatures = serializedIr.importedSignatures,
                )

                KotlinFileSerializedData(metadata, serializedIr.irData, serializedIr.irInlineData, serializedSignatureIndex)
            }

            if (processCompiledFileData != null) {
                val callableIds = serializedIr?.callableIdsOfInlineFuns.orEmpty()
                processCompiledFileData(ioFile, compiledKotlinFile, callableIds)
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

    val [fragmentNames, fragmentParts] = compiledKotlinFiles
        .groupBy { it.fqName }
        .map { [fqn, serializedData] ->
            fqn to serializedData.sortedBy { it.path }.map { it.metadata }
        }
        .sortedBy { it.first }
        .unzip()

    val metadataVersion = configuration.languageVersionSettings.languageVersion.toMetadataVersion().toArray()

    val serializedMetadata = SerializedMetadata(
        module = header,
        fragments = fragmentParts,
        fragmentNames = fragmentNames,
        metadataVersion = metadataVersion,
    )

    val signatureIndex: SerializedSignatureIndex? = runIf(serializedIrFromDirtySources != null) {
        for (file in cleanFiles) {
            val [export, import] = deserializeSignaturesIndex(file.signatureIndex ?: continue)
            globalExportedSignatures.addAll(export)
            globalImportedSignatures.addAll(import)
        }

        serializeSignaturesIndex(
            exportedSignatures = globalExportedSignatures,
            // Subtract to remove declarations referenced from other files in the same module
            importedSignatures = globalImportedSignatures - globalExportedSignatures,
        ).let(::SerializedSignatureIndex)
    }

    return SerializerOutput(
        serializedMetadata = serializedMetadata,
        serializedIr = if (serializedIrFromDirtySources == null) null
        else SerializedIrModule(
            signatureIndex = signatureIndex,
            compiledKotlinFiles.mapNotNull { it.irData },
            compiledKotlinFiles.mapNotNull { it.irInlineData },
        ),
        neededLibraries = dependencies,
    )
}

private fun serializeKlibHeader(
    languageVersionSettings: LanguageVersionSettings,
    moduleName: String,
    fragmentNames: List<String>,
    emptyPackages: List<String>
): KlibMetadataProtoBuf.Header {
    val header = KlibMetadataProtoBuf.Header.newBuilder()

    header.moduleName = moduleName

    addMetadataFlagsToHeader(header, languageVersionSettings)

    fragmentNames.forEach {
        header.addPackageFragmentName(it)
    }
    emptyPackages.forEach {
        header.addEmptyPackage(it)
    }

    return header.build()
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
        enabledFeatures.filter { it.forcesPreReleaseBinariesIfEnabled(languageVersionSettings.languageVersion) }.sortedBy(LanguageFeature::name).joinToString(" ") { "+$it" }
    if (presentablePoisoningFeatures.isNotBlank()) {
        manifestProperties.setProperty(KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES, presentablePoisoningFeatures)
    }

    if (languageVersionSettings.supportsFeature(LanguageFeature.CompanionBlocks)) {
        manifestProperties.setProperty(KLIB_PROPERTY_NEW_COMPANION_INITIALIZATION, true.toString())
    }
}
