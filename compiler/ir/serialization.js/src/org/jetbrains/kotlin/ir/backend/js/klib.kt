/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.serialization.KlibIrVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataIncrementalSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import java.io.File

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion ?: KlibMetadataVersion.INSTANCE

private val CompilerConfiguration.expectActualLinker: Boolean
    get() = get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

private fun getDescriptorForElement(
    context: BindingContext,
    element: PsiElement
): DeclarationDescriptor = BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

fun serializeModuleIntoKlib(
    moduleName: String,
    project: Project,
    configuration: CompilerConfiguration,
    messageLogger: IrMessageLogger,
    bindingContext: BindingContext,
    files: Collection<KtFile>,
    klibPath: String,
    dependencies: List<KotlinLibrary>,
    moduleFragment: IrModuleFragment,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    cleanFiles: List<KotlinFileSerializedData>,
    incrementalResultsConsumer: IncrementalResultsConsumer?,
    nopack: Boolean,
    perFile: Boolean,
    containsErrorCode: Boolean = false
) {
    assert(files.size == moduleFragment.files.size)

    val serializedIr =
        JsIrModuleSerializer(
            messageLogger,
            moduleFragment.irBuiltins,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            skipExpects = !configuration.expectActualLinker
        ).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor
    val metadataSerializer = KlibMetadataIncrementalSerializer(configuration, project, containsErrorCode)

    val empty = ByteArray(0)

    fun processCompiledFileData(ioFile: File, compiledFile: KotlinFileSerializedData) {
        incrementalResultsConsumer?.run {
            processPackagePart(ioFile, compiledFile.metadata, empty, empty)
            with(compiledFile.irData) {
                processIrFile(ioFile, fileData, types, signatures, strings, declarations, bodies, fqName.toByteArray())
            }
        }
    }

    val additionalFiles = mutableListOf<KotlinFileSerializedData>()

    for ((ktFile, binaryFile) in files.zip(serializedIr.files)) {
        assert(ktFile.virtualFilePath == binaryFile.path) {
            """The Kt and Ir files are put in different order
                Kt: ${ktFile.virtualFilePath}
                Ir: ${binaryFile.path}
            """.trimMargin()
        }
        val packageFragment = metadataSerializer.serializeScope(ktFile, bindingContext, moduleDescriptor)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)

        additionalFiles += compiledKotlinFile
        processCompiledFileData(VfsUtilCore.virtualToIoFile(ktFile.virtualFile), compiledKotlinFile)
    }

    val compiledKotlinFiles = (cleanFiles + additionalFiles)

    val header = metadataSerializer.serializeHeader(
        moduleDescriptor,
        compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
        emptyList()
    ).toByteArray()
    incrementalResultsConsumer?.run {
        processHeader(header)
    }

    val serializedMetadata =
        metadataSerializer.serializedMetadata(
            compiledKotlinFiles.groupBy { it.irData.fqName }.map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
            header
        )

    val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

    val versions = KotlinLibraryVersioning(
        abiVersion = KotlinAbiVersion.CURRENT,
        libraryVersion = null,
        compilerVersion = KotlinCompilerVersion.VERSION,
        metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
        irVersion = KlibIrVersion.INSTANCE.toString()
    )

    val properties = if (containsErrorCode) {
        Properties().also {
            it.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
        }
    } else null

    buildKotlinLibrary(
        linkDependencies = dependencies,
        ir = fullSerializedIr,
        metadata = serializedMetadata,
        dataFlowGraph = null,
        manifestProperties = properties,
        moduleName = moduleName,
        nopack = nopack,
        perFile = perFile,
        output = klibPath,
        versions = versions,
        builtInsPlatform = BuiltInsPlatform.JS
    )
}

fun KlibMetadataIncrementalSerializer.serializeScope(
    ktFile: KtFile,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor
): ProtoBuf.PackageFragment {
    val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
    return serializePackageFragment(moduleDescriptor, memberScope, ktFile.packageFqName)
}

fun KlibMetadataIncrementalSerializer(configuration: CompilerConfiguration, project: Project, allowErrors: Boolean) = KlibMetadataIncrementalSerializer(
    languageVersionSettings = configuration.languageVersionSettings,
    metadataVersion = configuration.metadataVersion,
    project = project,
    exportKDoc = false,
    skipExpects = !configuration.expectActualLinker,
    allowErrorTypes = allowErrors
)
