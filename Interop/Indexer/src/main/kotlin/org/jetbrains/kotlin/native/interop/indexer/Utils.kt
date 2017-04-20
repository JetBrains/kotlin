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

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*
import java.io.File

internal val CValue<CXType>.kind: CXTypeKind get() = this.useContents { kind }

internal val CValue<CXCursor>.kind: CXCursorKind get() = this.useContents { kind }

internal fun CValue<CXString>.convertAndDispose(): String {
    try {
        return clang_getCString(this)!!.toKString()
    } finally {
        clang_disposeString(this)
    }
}

internal fun getCursorSpelling(cursor: CValue<CXCursor>) =
        clang_getCursorSpelling(cursor).convertAndDispose()

internal fun CValue<CXType>.getSize(): Long {
    val size = clang_Type_getSizeOf(this)
    if (size < 0) {
        throw Error(size.toString())
    }
    return size
}

internal fun convertUnqualifiedPrimitiveType(type: CValue<CXType>): Type = when (type.kind) {
    CXTypeKind.CXType_Char_U, CXTypeKind.CXType_Char_S -> {
        assert(type.getSize() == 1L)
        CharType
    }

    CXTypeKind.CXType_UChar, CXTypeKind.CXType_UShort,
    CXTypeKind.CXType_UInt, CXTypeKind.CXType_ULong, CXTypeKind.CXType_ULongLong -> IntegerType(
            size = type.getSize().toInt(),
            isSigned = false,
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

    CXTypeKind.CXType_SChar, CXTypeKind.CXType_Short,
    CXTypeKind.CXType_Int, CXTypeKind.CXType_Long, CXTypeKind.CXType_LongLong -> IntegerType(
            size = type.getSize().toInt(),
            isSigned = true,
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

    CXTypeKind.CXType_Float, CXTypeKind.CXType_Double -> FloatingType(
            size = type.getSize().toInt(),
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

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
                compilerArgs.toNativeStringArray(memScope), compilerArgs.size,
                null, 0,
                options
        )!!

        return result
    }
}

internal fun NativeLibrary.parse(index: CXIndex, options: Int = 0): CXTranslationUnit =
        parseTranslationUnit(index, this.createTempSource(), this.compilerArgs, options)

internal data class Diagnostic(val severity: CXDiagnosticSeverity, val format: String,
                               val location: CValue<CXSourceLocation>)

internal fun CXTranslationUnit.getDiagnostics(): Sequence<Diagnostic> {
    val numDiagnostics = clang_getNumDiagnostics(this)
    return (0 until numDiagnostics).asSequence()
            .map { index ->
                val diagnostic = clang_getDiagnostic(this, index)
                try {
                    val severity = clang_getDiagnosticSeverity(diagnostic)

                    val format = clang_formatDiagnostic(diagnostic, clang_defaultDiagnosticDisplayOptions())
                            .convertAndDispose()

                    val location = clang_getDiagnosticLocation(diagnostic)

                    Diagnostic(severity, format, location)
                } finally {
                    clang_disposeDiagnostic(diagnostic)
                }
            }
}

internal fun CXTranslationUnit.getCompileErrors(): Sequence<String> =
        getDiagnostics().filter { it.isError() }.map { it.format }

private fun Diagnostic.isError() = (severity == CXDiagnosticSeverity.CXDiagnostic_Error) ||
        (severity == CXDiagnosticSeverity.CXDiagnostic_Fatal)

internal fun CXTranslationUnit.hasCompileErrors() = (this.getCompileErrors().firstOrNull() != null)

internal fun CXTranslationUnit.ensureNoCompileErrors(): CXTranslationUnit {
    val firstError = this.getCompileErrors().firstOrNull() ?: return this
    throw Error(firstError)
}

internal typealias CursorVisitor = (cursor: CValue<CXCursor>, parent: CValue<CXCursor>) -> CXChildVisitResult

internal fun visitChildren(parent: CValue<CXCursor>, visitor: CursorVisitor) {
    val visitorPtr = StableObjPtr.create(visitor)
    val clientData = visitorPtr.value
    clang_visitChildren(parent, staticCFunction { cursor, parent, clientData ->
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST")
        val visitor = StableObjPtr.fromValue(clientData!!).get() as CursorVisitor
        visitor(cursor, parent)
    }, clientData)
}

internal fun visitChildren(translationUnit: CXTranslationUnit, visitor: CursorVisitor) =
        visitChildren(clang_getTranslationUnitCursor(translationUnit), visitor)

internal fun CValue<CXCursor>.isLeaf(): Boolean {
    var hasChildren = false

    visitChildren(this) { _, _ ->
        hasChildren = true
        CXChildVisitResult.CXChildVisit_Break
    }

    return !hasChildren
}

internal fun List<String>.toNativeStringArray(placement: NativePlacement): CArrayPointer<CPointerVar<ByteVar>> {
    return placement.allocArray(this.size) { index ->
        this.value = this@toNativeStringArray[index].cstr.getPointer(placement)
    }
}

val NativeLibrary.preambleLines: List<String>
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
        val options = CXTranslationUnit_ForSerialization
        val translationUnit = this.parse(index, options).ensureNoCompileErrors()
        try {
            clang_saveTranslationUnit(translationUnit, precompiledHeader.absolutePath, 0)
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }

    return this.copy(
            includes = emptyList(),
            compilerArgs = this.compilerArgs + listOf("-include-pch", precompiledHeader.absolutePath)
    )
}

internal fun NativeLibrary.includesDeclaration(cursor: CValue<CXCursor>): Boolean {
    return if (this.excludeSystemLibs) {
        clang_Location_isInSystemHeader(clang_getCursorLocation(cursor)) == 0
    } else {
        true
    }
}

private fun CXTranslationUnit.getErrorLineNumbers(): Sequence<Int> =
        getDiagnostics().filter {
            it.isError()
        }.map {
            memScoped {
                val lineNumberVar = alloc<IntVar>()
                clang_getFileLocation(it.location, null, lineNumberVar.ptr, null, null)
                lineNumberVar.value
            }
        }

/**
 * For each list of lines, checks if the code fragment composed from these lines is compilable against given library.
 */
fun List<List<String>>.mapFragmentIsCompilable(originalLibrary: NativeLibrary): List<Boolean> {
    val library = originalLibrary
            .copy(compilerArgs = originalLibrary.compilerArgs + "-ferror-limit=0")
            .precompileHeaders()

    val indicesOfNonCompilable = mutableSetOf<Int>()

    val fragmentsToCheck = this.withIndex().toMutableList()

    val index = clang_createIndex(excludeDeclarationsFromPCH = 1, displayDiagnostics = 0)!!
    try {
        val sourceFile = library.createTempSource()
        val translationUnit = parseTranslationUnit(index, sourceFile, library.compilerArgs, options = 0)
                .ensureNoCompileErrors()

        try {
            while (fragmentsToCheck.isNotEmpty()) {
                // Combine all fragments to be checked in a single file:
                sourceFile.bufferedWriter().use { writer ->
                    writer.appendPreamble(library)
                    fragmentsToCheck.forEach {
                        it.value.forEach {
                            assert(!it.contains('\n'))
                            writer.appendln(it)
                        }
                    }
                }

                clang_reparseTranslationUnit(translationUnit, 0, null, 0)
                val errorLineNumbers = translationUnit.getErrorLineNumbers().toSet()

                // Retain only those fragments that contain compilation error locations:
                var lastLineNumber = library.preambleLines.size
                fragmentsToCheck.retainAll {
                    val firstLineNumber = lastLineNumber + 1
                    lastLineNumber += it.value.size
                    (firstLineNumber .. lastLineNumber).any { it in errorLineNumbers }
                }

                if (fragmentsToCheck.isNotEmpty()) {
                    // The first fragment is now known to be non-compilable.
                    val firstFragment = fragmentsToCheck.removeAt(0)
                    indicesOfNonCompilable.add(firstFragment.index)
                }

                // The remaining fragments was potentially influenced by the first one,
                // and thus require to be checked again.
            }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }

    return this.indices.map { it !in indicesOfNonCompilable }
}