/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("KlibDump")

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import kotlin.system.exitProcess

private fun exitWithError(message: String, exitCode: Int = 1): Nothing {
    System.err.println(message)
    exitProcess(exitCode)
}

private object StderrMessageLogger : IrMessageLogger {
    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        System.err.println(
            buildString {
                append(severity.name)
                if (location != null) {
                    append('(')
                    append(location.filePath)
                    append(':')
                    append(location.line)
                    append(':')
                    append(location.column)
                    append(')')
                }
                append(": ")
                append(message)
            }
        )
    }
}

private fun deserializeMetadata(klib: KotlinLibrary): ModuleDescriptor {
    val metadataFactories =
        KlibMetadataFactories({ DefaultBuiltIns.Instance }, NullFlexibleTypeDeserializer, PlatformDependentTypeTransformer.None)

    val module = metadataFactories.DefaultDeserializedDescriptorFactory.createDescriptor(
        library = klib,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        storageManager = LockBasedStorageManager.NO_LOCKS,
        builtIns = DefaultBuiltIns.Instance,
        packageAccessHandler = null,
        platform = null,
    )
    module.setDependencies(listOf(DefaultBuiltIns.Instance.builtInsModule, module))

    return module
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun deserializeModule(klib: KotlinLibrary, languageVersionSettings: LanguageVersionSettingsImpl): IrModuleFragment {
    val moduleDescriptor = deserializeMetadata(klib)
    val signatureComposer = IdSignatureDescriptor(KlibDumpDescriptorMangler)
    val symbolTable = SymbolTable(signatureComposer, IrFactoryImpl)
    val typeTranslator = TypeTranslatorImpl(symbolTable, languageVersionSettings, moduleDescriptor)
    val linker = KlibDumpIrLinker(
        currentModule = null,
        messageLogger = StderrMessageLogger,
        builtIns = IrBuiltInsOverDescriptors(DefaultBuiltIns.Instance, typeTranslator, symbolTable),
        symbolTable = symbolTable
    )

    val moduleFragment = linker.deserializeFullModule(moduleDescriptor, klib)
    linker.init(null, emptyList())
    ExternalDependenciesGenerator(symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies() // TODO: Is this needed?
    linker.postProcess()
    return moduleFragment
}

// NOTE: Running this function from IDEA is not supported. Please use the :kotlin-util-klib-dump:dumpKlib Gradle task.
// Example:
//     ./gradlew :kotlin-util-klib-dump:dumpKlib --args="/path/to/module.klib"
fun main(args: Array<String>) {
    val klibPath = args.getOrNull(0) ?: exitWithError("Please specify a path to the klib")
    val kotlinLibrary = resolveSingleFileKlib(File(klibPath), strategy = ToolingSingleFileKlibResolveStrategy)
    val irModule = deserializeModule(kotlinLibrary, LanguageVersionSettingsImpl.DEFAULT)
    println(irModule.dump())
}
