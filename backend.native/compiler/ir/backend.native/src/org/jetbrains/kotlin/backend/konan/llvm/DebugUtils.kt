/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.SourceManager.FileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal object DWARF {
    val producer                       = "konanc ${CompilerVersion.CURRENT} / kotlin-compiler: ${KotlinVersion.CURRENT}"
    /* TODO: from LLVM sources is unclear what runtimeVersion corresponds to term in terms of dwarf specification. */
    val dwarfVersionMetaDataNodeName  get() = "Dwarf Version".mdString()
    val dwarfDebugInfoMetaDataNodeName get() = "Debug Info Version".mdString()
    const val debugInfoVersion = 3 /* TODO: configurable? */
    /**
     * This is  the value taken from [DIFlags.FlagFwdDecl], to mark type declaration as
     * forward one.
     */
    const val flagsForwardDeclaration = 4

    fun runtimeVersion(config: KonanConfig) = when (config.debugInfoVersion()) {
        2 -> 0
        1 -> 2 /* legacy :/ */
        else -> TODO("unsupported debug info format version")
    }

    /**
     * Note: Kotlin language constant appears in DWARF v6, while modern linker fails to links DWARF other then [2;4],
     * that why we emit version 4 actually.
     */
    fun dwarfVersion(config : KonanConfig) = when (config.debugInfoVersion()) {
        1 -> 2
        2 -> 4 /* likely the most of the future kotlin native debug info format versions will emit DWARF v4 */
        else -> TODO("unsupported debug info format version")
    }

    fun language(config: KonanConfig) = when (config.debugInfoVersion()) {
        1 -> DwarfLanguage.DW_LANG_C89.value
        else -> DwarfLanguage.DW_LANG_Kotlin.value
    }
}

fun KonanConfig.debugInfoVersion():Int = configuration[KonanConfigKeys.DEBUG_INFO_VERSION] ?: 1

internal class DebugInfo internal constructor(override val context: Context):ContextUtils {
    val files = mutableMapOf<String, DIFileRef>()
    val subprograms = mutableMapOf<LLVMValueRef, DISubprogramRef>()
    /* Some functions are inlined on all callsites and body is eliminated by DCE, so there's no LLVM value */
    val inlinedSubprograms = mutableMapOf<IrFunction, DISubprogramRef>()
    var builder: DIBuilderRef? = null
    var module: DIModuleRef? = null
    var compilationUnit: DIScopeOpaqueRef? = null
    var objHeaderPointerType: DITypeOpaqueRef? = null
    var types = mutableMapOf<IrType, DITypeOpaqueRef>()

    val llvmTypes = mapOf<IrType, LLVMTypeRef>(
            context.irBuiltIns.booleanType to context.llvm.llvmInt8,
            context.irBuiltIns.byteType    to context.llvm.llvmInt8,
            context.irBuiltIns.charType    to context.llvm.llvmInt16,
            context.irBuiltIns.shortType   to context.llvm.llvmInt16,
            context.irBuiltIns.intType     to context.llvm.llvmInt32,
            context.irBuiltIns.longType    to context.llvm.llvmInt64,
            context.irBuiltIns.floatType   to context.llvm.llvmFloat,
            context.irBuiltIns.doubleType  to context.llvm.llvmDouble)
    val llvmTypeSizes = llvmTypes.map { it.key to LLVMSizeOfTypeInBits(llvmTargetData, it.value) }.toMap()
    val llvmTypeAlignments = llvmTypes.map {it.key to LLVMPreferredAlignmentOfType(llvmTargetData, it.value)}.toMap()
    val otherLlvmType = LLVMPointerType(int64Type, 0)!!
    val otherTypeSize = LLVMSizeOfTypeInBits(llvmTargetData, otherLlvmType)
    val otherTypeAlignment = LLVMPreferredAlignmentOfType(llvmTargetData, otherLlvmType)

    val compilerGeneratedFile by lazy {
        DICreateFile(builder, "<compiler-generated>", "")!!
    }
}

/**
 * File entry starts offsets from zero while dwarf number lines/column starting from 1.
 */
private val NO_SOURCE_FILE = "no source file"
private fun FileEntry.location(offset: Int, offsetToNumber: (Int) -> Int): Int {
    assert(offset != UNDEFINED_OFFSET)
    // Part "name.isEmpty() || name == NO_SOURCE_FILE" is an awful hack, @minamoto, please fix properly.
    if (offset == SYNTHETIC_OFFSET || name.isEmpty() || name == NO_SOURCE_FILE) return 1
    // lldb uses 1-based unsigned integers, so 0 is "no-info".
    val result = offsetToNumber(offset) + 1
    assert(result != 0)
    return result
}

internal fun FileEntry.line(offset: Int) = location(offset, this::getLineNumber)

internal fun FileEntry.column(offset: Int) = location(offset, this::getColumnNumber)

internal data class FileAndFolder(val file: String, val folder: String) {
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
    if (context.shouldContainAnyDebugInfo()) {
        val path = context.config.outputFile
            .toFileAndFolder()
        @Suppress("UNCHECKED_CAST")
        context.debugInfo.module   = DICreateModule(
                builder            = context.debugInfo.builder,
                scope              = null,
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
        val dwarfVersion = node(llvmTwo, DWARF.dwarfVersionMetaDataNodeName, Int32(DWARF.dwarfVersion(context.config)).llvm)
        val nodeDebugInfoVersion = node(llvmTwo, DWARF.dwarfDebugInfoMetaDataNodeName, Int32(DWARF.debugInfoVersion).llvm)
        val llvmModuleFlags = "llvm.module.flags"
        LLVMAddNamedMetadataOperand(context.llvmModule, llvmModuleFlags, dwarfVersion)
        LLVMAddNamedMetadataOperand(context.llvmModule, llvmModuleFlags, nodeDebugInfoVersion)
        val objHeaderType = DICreateStructType(
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
                refPlace      = null).cast<DITypeOpaqueRef>()
        context.debugInfo.objHeaderPointerType = dwarfPointerType(context, objHeaderType)
    }
}

@Suppress("UNCHECKED_CAST")
internal fun IrType.dwarfType(context: Context, targetData: LLVMTargetDataRef): DITypeOpaqueRef {
    when {
        this.computePrimitiveBinaryTypeOrNull() != null -> return debugInfoBaseType(context, targetData, this.render(), llvmType(context), encoding().value.toInt())
        else -> {
            return when {
                classOrNull != null || this.isTypeParameter() -> context.debugInfo.objHeaderPointerType!!
                else -> TODO("$this: Does this case really exist?")
            }
        }
    }
}

internal fun IrType.diType(context: Context, llvmTargetData: LLVMTargetDataRef): DITypeOpaqueRef =
        context.debugInfo.types.getOrPut(this) {
            dwarfType(context, llvmTargetData)
        }

@Suppress("UNCHECKED_CAST")
private fun debugInfoBaseType(context:Context, targetData:LLVMTargetDataRef, typeName:String, type:LLVMTypeRef, encoding:Int) = DICreateBasicType(
        context.debugInfo.builder, typeName,
        LLVMSizeOfTypeInBits(targetData, type),
        LLVMPreferredAlignmentOfType(targetData, type).toLong(), encoding) as DITypeOpaqueRef

internal val IrFunction.types:List<IrType>
    get() {
        val parameters = valueParameters.map { it.type }
        return listOf(returnType, *parameters.toTypedArray())
    }

internal fun IrType.size(context:Context) = context.debugInfo.llvmTypeSizes.getOrDefault(this, context.debugInfo.otherTypeSize)

internal fun IrType.alignment(context:Context) = context.debugInfo.llvmTypeAlignments.getOrDefault(this, context.debugInfo.otherTypeAlignment).toLong()

internal fun IrType.llvmType(context:Context): LLVMTypeRef = context.debugInfo.llvmTypes.getOrElse(this) {
    when(computePrimitiveBinaryTypeOrNull()) {
        PrimitiveBinaryType.BYTE -> context.llvm.llvmInt8
        PrimitiveBinaryType.SHORT -> context.llvm.llvmInt16
        PrimitiveBinaryType.INT -> context.llvm.llvmInt32
        PrimitiveBinaryType.LONG -> context.llvm.llvmInt64
        PrimitiveBinaryType.FLOAT -> context.llvm.llvmFloat
        PrimitiveBinaryType.DOUBLE -> context.llvm.llvmDouble
        PrimitiveBinaryType.VECTOR128 -> context.llvm.llvmVector128
        else -> context.debugInfo.otherLlvmType
    }
}

internal fun IrType.encoding(): DwarfTypeKind = when(computePrimitiveBinaryTypeOrNull()) {
    PrimitiveBinaryType.FLOAT -> DwarfTypeKind.DW_ATE_float
    PrimitiveBinaryType.DOUBLE -> DwarfTypeKind.DW_ATE_float
    PrimitiveBinaryType.BOOLEAN -> DwarfTypeKind.DW_ATE_boolean
    PrimitiveBinaryType.POINTER -> DwarfTypeKind.DW_ATE_address
    else -> {
        //TODO: not recursive.
        if (this.isUnsigned()) DwarfTypeKind.DW_ATE_unsigned
        else DwarfTypeKind.DW_ATE_signed
    }
}

internal fun alignTo(value:Long, align:Long):Long = (value + align - 1) / align * align

internal fun IrFunction.subroutineType(context: Context, llvmTargetData: LLVMTargetDataRef): DISubroutineTypeRef {
    val types = this@subroutineType.types
    return subroutineType(context, llvmTargetData, types)
}

internal fun subroutineType(context: Context, llvmTargetData: LLVMTargetDataRef, types: List<IrType>): DISubroutineTypeRef {
    return memScoped {
        DICreateSubroutineType(context.debugInfo.builder, allocArrayOf(
                types.map { it.diType(context, llvmTargetData) }),
                types.size)!!
    }
}

@Suppress("UNCHECKED_CAST")
private fun dwarfPointerType(context: Context, type: DITypeOpaqueRef) =
        DICreatePointerType(context.debugInfo.builder, type) as DITypeOpaqueRef

internal fun setupBridgeDebugInfo(context: Context, function: LLVMValueRef): LocationInfo? {
    if (!context.shouldContainLocationDebugInfo()) {
        return null
    }

    val file = context.debugInfo.compilerGeneratedFile

    // TODO: can we share the scope among all bridges?
    val scope: DIScopeOpaqueRef = DICreateFunction(
            builder = context.debugInfo.builder,
            scope = file.reinterpret(),
            name = function.name,
            linkageName = function.name,
            file = file,
            lineNo = 0,
            type = subroutineType(context, context.llvm.runtime.targetData, emptyList()), // TODO: use proper type.
            isLocal = 0,
            isDefinition = 1,
            scopeLine = 0
    )!!.also {
        DIFunctionAddSubprogram(function, it)
    }.reinterpret()

    return LocationInfo(scope, 1, 0)
}
