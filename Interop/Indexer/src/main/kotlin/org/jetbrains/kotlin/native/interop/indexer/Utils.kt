package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*
import java.io.File

internal fun CXString.convertAndDispose(): String {
    try {
        return clang_getCString(this)!!.asCString().toString()
    } finally {
        clang_disposeString(this)
    }
}

internal fun getCursorSpelling(cursor: CXCursor) = memScoped {
    clang_getCursorSpelling(cursor, memScope).convertAndDispose()
}

internal fun convertUnqualifiedPrimitiveType(type: CXType): Type = when (type.kind.value) {
    // TODO: is e.g. CXType_Int guaranteed to be int32_t?

    CXTypeKind.CXType_Char_U, CXTypeKind.CXType_UChar -> UInt8Type
    CXTypeKind.CXType_Char_S, CXTypeKind.CXType_SChar -> Int8Type

    CXTypeKind.CXType_UShort -> UInt16Type
    CXTypeKind.CXType_Short -> Int16Type

    CXTypeKind.CXType_UInt -> UInt32Type
    CXTypeKind.CXType_Int -> Int32Type

    CXTypeKind.CXType_ULong -> UIntPtrType
    CXTypeKind.CXType_Long -> IntPtrType

    CXTypeKind.CXType_ULongLong -> UInt64Type
    CXTypeKind.CXType_LongLong -> Int64Type

    CXTypeKind.CXType_Float -> Float32Type
    CXTypeKind.CXType_Double -> Float64Type

    else -> UnsupportedType
}

internal fun parseTranslationUnit(
        index: CXIndex,
        sourceFile: File,
        compilerArgs: List<String>,
        options: Int
): CXTranslationUnit {
    memScoped {
        val result = clang_parseTranslationUnit(
                index,
                sourceFile.absolutePath,
                compilerArgs.toNativeStringArray(memScope)[0].ptr, compilerArgs.size,
                null, 0,
                options
        )!!

        return result
    }
}

internal fun NativeLibrary.parse(index: CXIndex, options: Int = 0): CXTranslationUnit =
        parseTranslationUnit(index, this.createTempSource(), this.compilerArgs, options)

internal fun getCompileErrors(translationUnit: CXTranslationUnit): Sequence<String> {
    val numDiagnostics = clang_getNumDiagnostics(translationUnit)
    return (0 .. numDiagnostics - 1).asSequence()
            .map { index -> clang_getDiagnostic(translationUnit, index)!! }
            .filter {
                val severity = clang_getDiagnosticSeverity(it)
                severity == CXDiagnosticSeverity.CXDiagnostic_Error ||
                        severity == CXDiagnosticSeverity.CXDiagnostic_Fatal
            }.map {
                memScoped {
                    clang_formatDiagnostic(it, clang_defaultDiagnosticDisplayOptions(), memScope).convertAndDispose()
                }
            }
}

internal fun CXTranslationUnit.ensureNoCompileErrors(): CXTranslationUnit {
    val firstError = getCompileErrors(this).firstOrNull() ?: return this
    throw Error(firstError)
}

internal typealias CursorVisitor = (cursor: CXCursor, parent: CXCursor) -> CXChildVisitResult

internal fun visitChildren(parent: CXCursor, visitor: CursorVisitor) {
    val visitorPtr = StableObjPtr.create(visitor)
    val clientData = visitorPtr.value
    clang_visitChildren(parent, staticCFunction { cursor, parent, clientData ->
        val visitor = StableObjPtr.fromValue(clientData!!).get() as CursorVisitor
        visitor(cursor, parent)
    }, clientData)
}

internal fun visitChildren(translationUnit: CXTranslationUnit, visitor: CursorVisitor) {
    memScoped {
        visitChildren(clang_getTranslationUnitCursor(translationUnit, memScope), visitor)
    }
}

internal fun List<String>.toNativeStringArray(placement: NativePlacement): CArray<CPointerVar<CInt8Var>> {
    return placement.allocArray(this.size) { index ->
        this.value = this@toNativeStringArray[index].toCString(placement)!!.asCharPtr()
    }
}

internal val NativeLibrary.preambleLines: List<String>
    get() = this.includes.map { "#include <$it>" }

internal fun Appendable.appendPreamble(library: NativeLibrary) = this.apply {
    library.preambleLines.forEach {
        this.appendln(it)
    }
}

/**
 * Creates temporary source file which includes the library.
 */
internal fun NativeLibrary.createTempSource(): File {
    val suffix = when (language) {
        Language.C -> ".c"
    }
    val result = createTempFile(suffix = suffix)
    result.deleteOnExit()

    result.bufferedWriter().use { writer ->
        writer.appendPreamble(this)
    }

    return result
}

/**
 * Precompiles the headers of this library.
 *
 * @return the library which includes the precompiled header instead of original ones.
 */
internal fun NativeLibrary.precompileHeaders(): NativeLibrary {
    val precompiledHeader = createTempFile(suffix = ".pch").apply { this.deleteOnExit() }

    val index = clang_createIndex(excludeDeclarationsFromPCH = 0, displayDiagnostics = 0)!!
    try {
        val options = CXTranslationUnit_Flags.CXTranslationUnit_ForSerialization.value
        val translationUnit = this.parse(index, options).ensureNoCompileErrors()
        try {
            clang_saveTranslationUnit(translationUnit, precompiledHeader.absolutePath, 0)
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }

    return NativeLibrary(
            includes = emptyList(),
            compilerArgs = this.compilerArgs + listOf("-include-pch", precompiledHeader.absolutePath),
            language = this.language
    )
}