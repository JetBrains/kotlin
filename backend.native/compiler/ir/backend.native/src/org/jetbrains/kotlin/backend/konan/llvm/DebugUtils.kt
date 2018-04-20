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

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.backend.konan.irasdescriptors.FunctionDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.SourceManager.FileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils


internal object DWARF {
    val producer                       = "konanc ${KonanVersion.CURRENT} / kotlin-compiler: ${KotlinVersion.CURRENT}"
    /* TODO: from LLVM sources is unclear what runtimeVersion corresponds to term in terms of dwarf specification. */
    val runtimeVersion                 = 2
    val dwarfVersionMetaDataNodeName   = "Dwarf Name".mdString()
    val dwarfDebugInfoMetaDataNodeName = "Debug Info Version".mdString()
    val dwarfVersion = 2 /* TODO: configurable? like gcc/clang -gdwarf-2 and so on. */
    val debugInfoVersion = 3 /* TODO: configurable? */
    /**
     * This is  the value taken from [DIFlags.FlagFwdDecl], to mark type declaration as
     * forward one.
     */
    val flagsForwardDeclaration = 4
}

internal class DebugInfo internal constructor(override val context: Context):ContextUtils {
    val files = mutableMapOf<String, DIFileRef>()
    val subprograms = mutableMapOf<LLVMValueRef, DISubprogramRef>()
    var builder: DIBuilderRef? = null
    var module: DIModuleRef? = null
    var types = mutableMapOf<KotlinType, DITypeOpaqueRef>()

    val llvmTypes = mapOf<KotlinType, LLVMTypeRef>(
            context.builtIns.byteType    to LLVMInt8Type()!!,
            context.builtIns.charType    to LLVMInt8Type()!!,
            context.builtIns.shortType   to LLVMInt16Type()!!,
            context.builtIns.intType     to LLVMInt32Type()!!,
            context.builtIns.longType    to LLVMInt64Type()!!,
            context.builtIns.booleanType to LLVMInt1Type()!!,
            context.builtIns.floatType   to LLVMFloatType()!!,
            context.builtIns.doubleType  to LLVMDoubleType()!!)
    val intTypes = listOf<KotlinType>(context.builtIns.byteType, context.builtIns.shortType, context.builtIns.intType, context.builtIns.longType)
    val realTypes = listOf<KotlinType>(context.builtIns.floatType, context.builtIns.doubleType)
    val llvmTypeSizes = llvmTypes.map { it.key to LLVMSizeOfTypeInBits(llvmTargetData, it.value) }.toMap()
    val llvmTypeAlignments = llvmTypes.map {it.key to LLVMPreferredAlignmentOfType(llvmTargetData, it.value)}.toMap()
    val otherLlvmType = LLVMPointerType(LLVMInt64Type(), 0)!!
    val otherTypeSize = LLVMSizeOfTypeInBits(llvmTargetData, otherLlvmType)
    val otherTypeAlignment = LLVMPreferredAlignmentOfType(llvmTargetData, otherLlvmType)
}
/**
 * File entry starts offsets from zero while dwarf number lines/column starting from 1.
 */
private fun FileEntry.location(offset:Int, offsetToNumber:(Int) -> Int):Int {
    return if (offset < 0) 0 // lldb uses 1-based unsigned integers, so 0 is "no-info"
    else offsetToNumber(offset) + 1
}

internal fun FileEntry.line(offset: Int) = location(offset, this::getLineNumber)

internal fun FileEntry.column(offset: Int) = location(offset, this::getColumnNumber)

internal data class FileAndFolder(val file:String, val folder:String) {
    companion object {
        val NOFILE =  FileAndFolder("-", "")
    }

    fun path() = if (this == NOFILE) file else "$folder/$file"
}

internal fun String?.toFileAndFolder():FileAndFolder {
    this ?: return FileAndFolder.NOFILE
    val file = File(this).absoluteFile
    return FileAndFolder(file.name, file.parent)
}

internal fun generateDebugInfoHeader(context: Context) {
    if (context.shouldContainDebugInfo()) {

        val path = context.config.outputFile
            .toFileAndFolder()
        @Suppress("UNCHECKED_CAST")
        context.debugInfo.module   = DICreateModule(
                builder            = context.debugInfo.builder,
                scope              = context.llvmModule as DIScopeOpaqueRef,
                name               = path.path(),
                configurationMacro = "",
                includePath        = "",
                iSysRoot           = "")
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

@Suppress("UNCHECKED_CAST")
internal fun KotlinType.dwarfType(context:Context, targetData:LLVMTargetDataRef): DITypeOpaqueRef {
    when {
        KotlinBuiltIns.isPrimitiveType(this) -> return debugInfoBaseType(context, targetData, this.getJetTypeFqName(false), llvmType(context), encoding(context).value.toInt())
        else -> {
            val classDescriptor = TypeUtils.getClassDescriptor(this)
            return when {
                classDescriptor != null -> {
                    val type = DICreateStructType(
                            refBuilder    = context.debugInfo.builder,
                            // TODO: here should be DIFile as scope.
                            scope         = null,
                            name          = "ObjHeader",
                            file          = null,
                            lineNumber    = 0,
                            sizeInBits    = 0,
                            alignInBits   = 0,
                            flags         = DWARF.flagsForwardDeclaration,
                            derivedFrom   = null,
                            elements      = null,
                            elementsCount = 0,
                            refPlace      = null)!! as DITypeOpaqueRef
                    dwarfPointerType(context, type)
                }
                TypeUtils.isTypeParameter(this) -> //TODO: Type parameter,  how to deal with if?
                    debugInfoBaseType(context, targetData, this.toString(), llvmType(context), encoding(context).value.toInt())
                else -> TODO("$this: Does this case really exist?")
            }
        }
    }
}

internal fun KotlinType.diType(context: Context, llvmTargetData: LLVMTargetDataRef): DITypeOpaqueRef =
        context.debugInfo.types.getOrPut(this) {
            dwarfType(context, llvmTargetData)
        }


@Suppress("UNCHECKED_CAST")
private fun debugInfoBaseType(context:Context, targetData:LLVMTargetDataRef, typeName:String, type:LLVMTypeRef, encoding:Int) = DICreateBasicType(
        context.debugInfo.builder, typeName,
        LLVMSizeOfTypeInBits(targetData, type),
        LLVMPreferredAlignmentOfType(targetData, type).toLong(), encoding) as DITypeOpaqueRef

internal val FunctionDescriptor.types:List<KotlinType>
    get() {
        val parameters = valueParameters.map{it.type}
        return listOf(returnType, *parameters.toTypedArray())
    }

internal fun KotlinType.size(context:Context) = context.debugInfo.llvmTypeSizes.getOrDefault(this, context.debugInfo.otherTypeSize)

internal fun KotlinType.alignment(context:Context) = context.debugInfo.llvmTypeAlignments.getOrDefault(this, context.debugInfo.otherTypeAlignment).toLong()

internal fun KotlinType.llvmType(context:Context): LLVMTypeRef = context.debugInfo.llvmTypes.getOrDefault(this, context.debugInfo.otherLlvmType)

private fun<T> or(v:T, vararg p:(T)->Boolean):Boolean = p.any{it(v)}

internal fun KotlinType.encoding(context:Context):DwarfTypeKind = when {
    this in context.debugInfo.intTypes            -> DwarfTypeKind.DW_ATE_signed
    this in context.debugInfo.realTypes           -> DwarfTypeKind.DW_ATE_float
    KotlinBuiltIns.isBoolean(this)                -> DwarfTypeKind.DW_ATE_boolean
    KotlinBuiltIns.isChar(this)                   -> DwarfTypeKind.DW_ATE_unsigned
    (!KotlinBuiltIns.isPrimitiveType(this))       -> DwarfTypeKind.DW_ATE_address
    else                                          -> TODO(toString())
}

internal fun alignTo(value:Long, align:Long):Long = (value + align - 1) / align * align

internal fun  FunctionDescriptor.subroutineType(context: Context, llvmTargetData: LLVMTargetDataRef): DISubroutineTypeRef {
    val types = this@subroutineType.types
    return subroutineType(context, llvmTargetData, types)
}

internal fun subroutineType(context: Context, llvmTargetData: LLVMTargetDataRef, types: List<KotlinType>): DISubroutineTypeRef {
    return memScoped {
        DICreateSubroutineType(context.debugInfo.builder, allocArrayOf(
                types.map { it.diType(context, llvmTargetData) }),
                types.size)!!
    }
}

@Suppress("UNCHECKED_CAST")
private fun dwarfPointerType(context: Context, type: DITypeOpaqueRef) =
        DICreatePointerType(context.debugInfo.builder, type) as DITypeOpaqueRef
