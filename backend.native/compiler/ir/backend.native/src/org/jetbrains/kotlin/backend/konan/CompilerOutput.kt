/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import llvm.LLVMLinkModules2
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

val CompilerOutputKind.isNativeBinary: Boolean get() = when (this) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC, CompilerOutputKind.FRAMEWORK -> true
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
}

internal fun produceOutput(context: Context, phaser: PhaseManager) {

    val llvmModule = context.llvmModule!!
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

            phaser.phase(KonanPhase.BITCODE_LINKER) {
                for (library in nativeLibraries) {
                    val libraryModule = parseBitcodeFile(library)
                    val failed = LLVMLinkModules2(llvmModule, libraryModule)
                    if (failed != 0) {
                        throw Error("failed to link $library") // TODO: retrieve error message from LLVM.
                    }
                }
            }

            LLVMWriteBitcodeToFile(llvmModule, output)
        }
        CompilerOutputKind.LIBRARY -> {
            val output = context.config.outputFiles.outputName
            val libraryName = context.config.moduleId
            val neededLibraries 
                = context.llvm.librariesForLibraryManifest
            val abiVersion = context.config.currentAbiVersion
            val target = context.config.target
            val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
            val manifest = config.get(KonanConfigKeys.MANIFEST_FILE)

            val library = buildLibrary(
                context.config.nativeLibraries, 
                context.config.includeBinaries,
                neededLibraries,
                context.serializedLinkData!!, 
                abiVersion,
                target,
                output,
                libraryName, 
                llvmModule,
                nopack,
                manifest,
                context.dataFlowGraph)

            context.library = library
            context.bitcodeFileName = library.mainBitcodeFileName
        }
        CompilerOutputKind.BITCODE -> {
            val output = context.config.outputFile
            context.bitcodeFileName = output
            LLVMWriteBitcodeToFile(llvmModule, output)
        }
    }
}


