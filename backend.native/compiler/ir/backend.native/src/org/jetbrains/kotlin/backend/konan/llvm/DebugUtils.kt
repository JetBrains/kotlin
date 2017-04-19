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

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.DICreateCompilationUnit
import llvm.DICreateModule
import llvm.DIScopeOpaqueRef
import llvm.LLVMAddNamedMetadataOperand
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.KonanVersion
import org.jetbrains.kotlin.ir.SourceManager.FileEntry


internal object DWARF {
    val DW_LANG_kotlin                 = 1 //TODO: we need own constant e.g. 0xbabe
    val producer                       = "konanc ${KonanVersion.CURRENT} / kotlin-compiler: ${KotlinVersion.CURRENT}"
    /* TODO: from LLVM sources is unclear what runtimeVersion corresponds to term in terms of dwarf specification. */
    val runtimeVersion                 = 2
    val dwarfVersionMetaDataNodeName   = "Dwarf Name".mdString()
    val dwarfDebugInfoMetaDataNodeName = "Debug Info Version".mdString()
    val dwarfVersion = 2 /* TODO: configurable? like gcc/clang -gdwarf-2 and so on. */
    val debugInfoVersion = 3 /* TODO: configurable? */
}


/**
 * File entry starts offsets from zero while dwarf number lines/column starting from 1.
 */
private fun FileEntry.location(offset:Int, offsetToNumber:(Int) -> Int):Int {
    return if (offset < 0) -1
    else offsetToNumber(offset) + 1
}

internal fun FileEntry.line(offset: Int) = location(offset, this::getLineNumber)

internal fun FileEntry.column(offset: Int) = location(offset, this::getColumnNumber)

internal fun generateDebugInfoHeader(context: Context) {
    if (context.shouldContainDebugInfo()) {
        context.debugInfo.module = DICreateModule(
                builder = context.debugInfo.builder,
                scope = context.llvmModule as DIScopeOpaqueRef,
                name = context.config.configuration.get(KonanConfigKeys.BITCODE_FILE)!!,
                configurationMacro = "",
                includePath = "",
                iSysRoot = "")
        context.debugInfo.compilationModule = DICreateCompilationUnit(
                builder = context.debugInfo.builder,
                lang = DWARF.DW_LANG_kotlin,
                File = context.config.configuration.get(KonanConfigKeys.BITCODE_FILE)!!,
                dir = "",
                producer = DWARF.producer,
                isOptimized = 0,
                flags = "",
                rv = DWARF.runtimeVersion)
        /* TODO: figure out what here 2 means:
         *
         * 0:b-backend-dwarf:minamoto@minamoto-osx(0)# cat /dev/null | clang -xc -S -emit-llvm -g -o - -
         * ; ModuleID = '-'
         * source_filename = "-"
         * target datalayout = "e-m:o-i64:64-f80:128-n8:16:32:64-S128"
         * target triple = "x86_64-apple-macosx10.12.0"
         *
         * !llvm.dbg.cu = !{!0}
         * !llvm.module.flags = !{!3, !4, !5}
         * !llvm.ident = !{!6}
         *
         * !0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: "Apple LLVM version 8.0.0 (clang-800.0.38)", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug, enums: !2)
         * !1 = !DIFile(filename: "-", directory: "/Users/minamoto/ws/.git-trees/backend-dwarf")
         * !2 = !{}
         * !3 = !{i32 2, !"Dwarf Version", i32 2}              ; <-
         * !4 = !{i32 2, !"Debug Info Version", i32 700000003} ; <-
         * !5 = !{i32 1, !"PIC Level", i32 2}
         * !6 = !{!"Apple LLVM version 8.0.0 (clang-800.0.38)"}
         */
        val llvmTwo = Int32(2).llvm
        val dwarfVersion = node(llvmTwo, DWARF.dwarfVersionMetaDataNodeName, Int32(DWARF.dwarfVersion).llvm)
        val nodeDebugInfoVersion = node(llvmTwo, DWARF.dwarfDebugInfoMetaDataNodeName, Int32(DWARF.debugInfoVersion).llvm)
        val llvmModuleFlags = "llvm.module.flags"
        LLVMAddNamedMetadataOperand(context.llvmModule, llvmModuleFlags, dwarfVersion)
        LLVMAddNamedMetadataOperand(context.llvmModule, llvmModuleFlags, nodeDebugInfoVersion)
    }
}
