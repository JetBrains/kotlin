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

/**
 * Finds all "macro constants" and registers them as [NativeIndex.constants] in given index.
 */
internal fun findMacros(nativeIndex: NativeIndexImpl, translationUnit: CXTranslationUnit, headers: Set<CXFile?>) {
    val names = collectMacroNames(nativeIndex, translationUnit, headers)
    // TODO: apply user-defined filters.
    val macros = expandMacros(nativeIndex.library, names, typeConverter = { nativeIndex.convertType(it) })

    macros.filterIsInstanceTo(nativeIndex.macroConstants)
    macros.filterIsInstanceTo(nativeIndex.wrappedMacros)
}

private typealias TypeConverter = (CValue<CXType>) -> Type

/**
 * For each name expands the macro with this name declared in the library,
 * checking if it gets expanded to a constant expression.
 *
 * @return the list of constants.
 */
private fun expandMacros(
        originalLibrary: NativeLibrary,
        names: List<String>,
        typeConverter: TypeConverter
): List<MacroDef> {

    // Each macro is expanded by parsing it separately with the library;
    // so precompile library headers to significantly speed up the parsing:
    val library = originalLibrary.precompileHeaders()

    withIndex(excludeDeclarationsFromPCH = true) { index ->
        val sourceFile = library.createTempSource()
        // We disable implicit function declaration to filter out cases when a macro is expanded as a function
        // or function-like construction (e.g. #define FOO throw()) but such a function is undeclared.
        val compilerArgs = library.compilerArgs + listOf("-Werror=implicit-function-declaration")
        val translationUnit = parseTranslationUnit(index, sourceFile, compilerArgs, options = 0)
        try {
            translationUnit.ensureNoCompileErrors()
            return names.mapNotNull {
                expandMacro(library, translationUnit, sourceFile, it, typeConverter)
            }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }
}

/**
 * Expands the macro [name] defined in [library].
 * Returns the resulting constant or `null` if the result is not a constant (expression).
 *
 * As a side effect, modifies the [sourceFile] and reparses the [translationUnit].
 */
private fun expandMacro(
        library: NativeLibrary,
        translationUnit: CXTranslationUnit,
        sourceFile: File,
        name: String,
        typeConverter: TypeConverter
): MacroDef? {

    reparseWithCodeSnippet(library, translationUnit, sourceFile, name)

    if (!translationUnit.hasCompileErrors()) {
        return processCodeSnippet(translationUnit, name, typeConverter)
    } else {
        return null
    }
}

/**
 * Adds the code snippet to be then processed with [processCodeSnippet] to the [sourceFile]
 * and reparses the [translationUnit].
 *
 *  - If the code snippet allows extracting the constant value using libclang API, we'll add a [ConstantDef] in the
 * native index and generate a Kotlin constant for it.
 *  - If the expression type can be inferred by libclang, we'll add a [WrappedMacroDef] in the native index and
 * generate a bridge for this macro.
 *  - Otherwise the macro is skipped.
 */
private fun reparseWithCodeSnippet(library: NativeLibrary,
                                   translationUnit: CXTranslationUnit, sourceFile: File,
                                   name: String) {

    // TODO: consider using CXUnsavedFile instead of writing the modified file to OS file system.
    sourceFile.bufferedWriter().use { writer ->
        writer.appendPreamble(library)

        // Note: clang_Cursor_Evaluate permits expression to have side-effects,
        // so the code pattern should force the constant evaluation that corresponds to language rules.
        val codeSnippetLines = when (library.language) {
            // Note: __auto_type is a GNU extension which is supported by clang.
            Language.C, Language.OBJECTIVE_C -> listOf(
                    "void kni_indexer_function() { __auto_type KNI_INDEXER_VARIABLE = $name; }"
            )
        }

        codeSnippetLines.forEach { writer.append(it) }
    }
    clang_reparseTranslationUnit(translationUnit, 0, null, 0)
}

/**
 * Checks that [translationUnit] is parsed exactly as expected for the code appended by [reparseWithCodeSnippet],
 * and returns the constant on success.
 */
private fun processCodeSnippet(
        translationUnit: CXTranslationUnit,
        name: String,
        typeConverter: TypeConverter
): MacroDef? {

    val kindsToSkip = listOf(CXCursorKind.CXCursor_FunctionDecl, CXCursorKind.CXCursor_CompoundStmt)
    var state = VisitorState.EXPECT_NODES_TO_SKIP
    var evalResultOrNull: CXEvalResult? = null
    var typeOrNull: Type? = null

    val visitor: CursorVisitor = { cursor, _ ->
        val kind = cursor.kind
        when {
            state == VisitorState.EXPECT_VARIABLE && kind == CXCursorKind.CXCursor_VarDecl -> {
                evalResultOrNull = clang_Cursor_Evaluate(cursor)
                state = VisitorState.EXPECT_VARIABLE_VALUE
                CXChildVisitResult.CXChildVisit_Recurse
            }

            state == VisitorState.EXPECT_VARIABLE_VALUE && clang_isExpression(kind) != 0 -> {
                typeOrNull = typeConverter(clang_getCursorType(cursor))
                state = VisitorState.EXPECT_END
                CXChildVisitResult.CXChildVisit_Continue
            }

            // Skip auxiliary elements.
            state == VisitorState.EXPECT_NODES_TO_SKIP && kind in kindsToSkip ->
                CXChildVisitResult.CXChildVisit_Recurse

            state == VisitorState.EXPECT_NODES_TO_SKIP && kind == CXCursorKind.CXCursor_DeclStmt -> {
                state = VisitorState.EXPECT_VARIABLE
                CXChildVisitResult.CXChildVisit_Recurse
            }

            else -> {
                state = VisitorState.INVALID
                CXChildVisitResult.CXChildVisit_Break
            }
        }
    }

    try {
        visitChildren(translationUnit, visitor)

        if (state != VisitorState.EXPECT_END) {
            return null
        }

        val type = typeOrNull!!
        return if (evalResultOrNull == null) {
            // The macro cannot be evaluated as a constant so we will wrap it in a bridge.
            when(type.unwrapTypedefs()) {
                is PrimitiveType,
                is PointerType,
                is ObjCPointer -> WrappedMacroDef(name, type)
                else -> null
            }
        } else {
            // Otherwise we can evaluate the expression and create a Kotlin constant for it.
            val evalResult = evalResultOrNull!!
            val evalResultKind = clang_EvalResult_getKind(evalResult)
            when (evalResultKind) {
                CXEvalResultKind.CXEval_Int ->
                    IntegerConstantDef(name, type, clang_EvalResult_getAsLongLong(evalResult))

                CXEvalResultKind.CXEval_Float ->
                    FloatingConstantDef(name, type, clang_EvalResult_getAsDouble(evalResult))

                CXEvalResultKind.CXEval_CFStr,
                CXEvalResultKind.CXEval_ObjCStrLiteral,
                CXEvalResultKind.CXEval_StrLiteral ->
                    if (evalResultKind == CXEvalResultKind.CXEval_StrLiteral && !type.canonicalIsPointerToChar()) {
                        // libclang doesn't seem to support wide string literals properly in this API;
                        // thus disable wide literals here:
                        null
                    } else {
                        StringConstantDef(name, type, clang_EvalResult_getAsStr(evalResult)!!.toKString())
                    }

                CXEvalResultKind.CXEval_Other,
                CXEvalResultKind.CXEval_UnExposed -> null
            }
        }

    } finally {
        evalResultOrNull?.let { clang_EvalResult_dispose(it) }
    }
}

enum class VisitorState {
    EXPECT_NODES_TO_SKIP,
    EXPECT_VARIABLE, EXPECT_VARIABLE_VALUE,
    EXPECT_END, INVALID
}

private fun collectMacroNames(nativeIndex: NativeIndexImpl, translationUnit: CXTranslationUnit, headers: Set<CXFile?>): List<String> {
    val result = mutableSetOf<String>()

    visitChildren(translationUnit) { cursor, _ ->
        val file = memScoped {
            val fileVar = alloc<CXFileVar>()
            clang_getFileLocation(clang_getCursorLocation(cursor), fileVar.ptr, null, null, null)
            fileVar.value
        }

        if (cursor.kind == CXCursorKind.CXCursor_MacroDefinition &&
                nativeIndex.library.includesDeclaration(cursor) &&
                file != null && // Builtin macros mostly seem to be useless.
                file in headers &&
                canMacroBeConstant(cursor))
        {
            val spelling = getCursorSpelling(cursor)
            result.add(spelling)
        }
        CXChildVisitResult.CXChildVisit_Continue
    }

    return result.toList()
}

private fun canMacroBeConstant(cursor: CValue<CXCursor>): Boolean {

    if (clang_Cursor_isMacroFunctionLike(cursor) != 0) {
        return false
    }

    // TODO: check number of tokens and filter out empty definitions;
    // Requires updating to 3.9.1 due to https://bugs.llvm.org//show_bug.cgi?id=9069

    return true
}