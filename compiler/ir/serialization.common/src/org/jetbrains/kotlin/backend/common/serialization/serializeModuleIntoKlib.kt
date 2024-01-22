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
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
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
    val dataFlowGraph: ByteArray?,
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
 * @param compatibilityMode The information about KLIB ABI.
 * @param cleanFiles In the case of incremental compilation, the list of files that were not changed and therefore don't need to be
 *     serialized again.
 * @param dependencies The list of KLIBs that the KLIB being produced depends on.
 * @param createModuleSerializer Used for creating a backend-specific instance of [IrModuleSerializer].
 * @param metadataSerializer Something capable of serializing the metadata of the source files. See the corresponding interface KDoc.
 * @param runKlibCheckers Additional checks to be run before serializing [irModuleFragment]. Can be used to report serialization-time
 *     diagnostics.
 * @param processCompiledFileData Called for each newly serialized file. Useful for incremental compilation.
 * @param processKlibHeader Called after serializing the KLIB header. Useful for incremental compilation.
 */
fun <Dependency : KotlinLibrary, SourceFile> serializeModuleIntoKlib(
    moduleName: String,
    irModuleFragment: IrModuleFragment?,
    configuration: CompilerConfiguration,
    diagnosticReporter: DiagnosticReporter,
    compatibilityMode: CompatibilityMode,
    cleanFiles: List<KotlinFileSerializedData>,
    dependencies: List<Dependency>,
    createModuleSerializer: (
        irDiagnosticReporter: IrDiagnosticReporter,
        irBuiltins: IrBuiltIns,
        compatibilityMode: CompatibilityMode,
        normalizeAbsolutePaths: Boolean,
        sourceBaseDirs: Collection<String>,
        languageVersionSettings: LanguageVersionSettings,
        shouldCheckSignaturesOnUniqueness: Boolean,
    ) -> IrModuleSerializer<*>,
    metadataSerializer: KlibSingleFileMetadataSerializer<SourceFile>,
    runKlibCheckers: (IrModuleFragment, IrDiagnosticReporter, CompilerConfiguration) -> Unit = { _, _, _ -> },
    processCompiledFileData: ((File, KotlinFileSerializedData) -> Unit)? = null,
    processKlibHeader: (ByteArray) -> Unit = {},
): SerializerOutput<Dependency> {
    if (irModuleFragment != null) {
        assert(metadataSerializer.numberOfSourceFiles == irModuleFragment.files.size) {
            "The number of source files (${metadataSerializer.numberOfSourceFiles}) does not match the number of IrFiles (${irModuleFragment.files.size})"
        }
    }

    val sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES] ?: emptyList()
    val normalizeAbsolutePath = configuration.getBoolean(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH)
    val signatureClashChecks = configuration[CommonConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS] ?: true

    val serializedIr = irModuleFragment?.let {
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings)
        runKlibCheckers(it, irDiagnosticReporter, configuration)
        createModuleSerializer(
            irDiagnosticReporter,
            it.irBuiltins,
            compatibilityMode,
            normalizeAbsolutePath,
            sourceBaseDirs,
            configuration.languageVersionSettings,
            signatureClashChecks,
        ).serializedIrModule(it)
    }

    val serializedFiles = serializedIr?.files?.toList()

    val compiledKotlinFiles = buildList {
        addAll(cleanFiles)
        metadataSerializer.forEachFile { i, sourceFile, ktSourceFile, packageFqName ->
            val binaryFile = serializedFiles?.get(i)?.also {
                assert(ktSourceFile.path == it.path) {
                    """The Kt and Ir files are put in different order
                    Kt: ${ktSourceFile.path}
                    Ir: ${it.path}
                    """.trimMargin()
                }
            }
            val protoBuf = metadataSerializer.serializeSingleFileMetadata(sourceFile)
            val metadata = protoBuf.toByteArray()
            val compiledKotlinFile = if (binaryFile == null)
                KotlinFileSerializedData(metadata, ktSourceFile.path, packageFqName.asString())
            else
                KotlinFileSerializedData(metadata, binaryFile)

            if (processCompiledFileData != null) {
                val ioFile = ktSourceFile.toIoFileOrNull() ?: error("No file found for source ${ktSourceFile.path}")
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

    val serializedMetadata = SerializedMetadata(
        module = header,
        fragments = fragmentParts,
        fragmentNames = fragmentNames
    )

    return SerializerOutput(
        serializedMetadata = serializedMetadata,
        serializedIr = if (serializedIr == null) null else SerializedIrModule(compiledKotlinFiles.mapNotNull { it.irData }),
        dataFlowGraph = null,
        neededLibraries = dependencies,
    )
}
