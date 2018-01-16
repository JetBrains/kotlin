/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.backend.konan

import llvm.LLVMLinkModules2
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.backend.konan.util.getValueOrNull
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

val CompilerOutputKind.isNativeBinary: Boolean get() = when (this) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC, CompilerOutputKind.FRAMEWORK -> true
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
}

internal fun produceOutput(context: Context) {

    val llvmModule = context.llvmModule!!
    val config = context.config.configuration
    val tempFiles = context.config.tempFiles
    val produce = config.get(KonanConfigKeys.PRODUCE)

    when (produce) {
        CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.FRAMEWORK,
        CompilerOutputKind.PROGRAM -> {
            val output = tempFiles.nativeBinaryFileName
            context.bitcodeFileName = output

            val generatedBitcodeFiles = 
                if (produce == CompilerOutputKind.DYNAMIC) {
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

            PhaseManager(context).phase(KonanPhase.BITCODE_LINKER) {
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
            val output = context.config.outputName
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


