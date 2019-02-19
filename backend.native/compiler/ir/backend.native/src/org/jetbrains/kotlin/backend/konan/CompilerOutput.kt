/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import llvm.LLVMLinkModules2
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.konan.KonanAbiVersion
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.library.KonanLibraryVersioning
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

val CompilerOutputKind.isNativeBinary: Boolean get() = when (this) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC, CompilerOutputKind.FRAMEWORK -> true
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
}

internal fun produceCStubs(context: Context) {
    val llvmModule = context.llvmModule!!
    context.cStubsManager.compile(context.config.clang, context.messageCollector, context.inVerbosePhase)?.let {
        parseAndLinkBitcodeFile(llvmModule, it.absolutePath)
    }
}

internal fun produceOutput(context: Context) {

    val config = context.config.configuration
    val tempFiles = context.config.tempFiles
    val produce = config.get(KonanConfigKeys.PRODUCE)

    when (produce) {
        CompilerOutputKind.STATIC,
        CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.FRAMEWORK,
        CompilerOutputKind.PROGRAM -> {
            val output = tempFiles.nativeBinaryFileName
            context.bitcodeFileName = output

            val generatedBitcodeFiles =
                if (produce == CompilerOutputKind.DYNAMIC || produce == CompilerOutputKind.STATIC) {
                    produceCAdapterBitcode(
                        context.config.clang, 
                        tempFiles.cAdapterCppName, 
                        tempFiles.cAdapterBitcodeName)
                    listOf(tempFiles.cAdapterBitcodeName)
                } else emptyList()

            val nativeLibraries = 
                context.config.nativeLibraries +
                context.config.defaultNativeLibraries + 
                generatedBitcodeFiles

            for (library in nativeLibraries) {
                parseAndLinkBitcodeFile(context.llvmModule!!, library)
            }

            LLVMWriteBitcodeToFile(context.llvmModule!!, output)
        }
        CompilerOutputKind.LIBRARY -> {
            val output = context.config.outputFiles.outputName
            val libraryName = context.config.moduleId
            val neededLibraries = context.librariesWithDependencies
            val abiVersion = KonanAbiVersion.CURRENT
            val compilerVersion = KonanVersion.CURRENT
            val libraryVersion = config.get(KonanConfigKeys.LIBRARY_VERSION)
            val versions = KonanLibraryVersioning(abiVersion = abiVersion, libraryVersion = libraryVersion, compilerVersion = compilerVersion)
            val target = context.config.target
            val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
            val manifestProperties = context.config.manifestProperties


            val library = buildLibrary(
                context.config.nativeLibraries, 
                context.config.includeBinaries,
                neededLibraries,
                context.serializedLinkData!!,
                versions,
                target,
                output,
                libraryName, 
                null,
                nopack,
                manifestProperties,
                context.dataFlowGraph)

            context.library = library
            context.bitcodeFileName = library.mainBitcodeFileName
        }
        CompilerOutputKind.BITCODE -> {
            val output = context.config.outputFile
            context.bitcodeFileName = output
            LLVMWriteBitcodeToFile(context.llvmModule!!, output)
        }
    }
}

private fun parseAndLinkBitcodeFile(llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(path)
    val failed = LLVMLinkModules2(llvmModule, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $path") // TODO: retrieve error message from LLVM.
    }
}
