/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("KlibDump")

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
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

private fun deserializeModule(klib: KotlinLibrary): IrModuleFragment {
    val libraryProto = parseModuleHeader(klib.moduleHeaderData)
    val moduleName = Name.special(libraryProto.moduleName)
    val signatureComposer = IdSignatureDescriptor(KlibDumpDescriptorMangler)
    val linker = KlibDumpIrLinker(
        DummyModuleDescriptor(moduleName),
        StderrMessageLogger,
        KlibDumpBuiltins(LanguageVersionSettingsImpl.DEFAULT), // TODO: Are these settings right?
        SymbolTable(signatureComposer, IrFactoryImpl)
    )
    val moduleFragment = linker.deserializeFullModule(DummyModuleDescriptor(moduleName), klib)
    linker.init(null, emptyList())
    linker.postProcess()
    return moduleFragment
}

fun main(args: Array<String>) {
    val klibPath = args.getOrNull(0) ?: exitWithError("Please specify a path to the klib")
    val kotlinLibrary = resolveSingleFileKlib(File(klibPath), strategy = ToolingSingleFileKlibResolveStrategy)
    val irModule = deserializeModule(kotlinLibrary)
    println(irModule.dump())
}
