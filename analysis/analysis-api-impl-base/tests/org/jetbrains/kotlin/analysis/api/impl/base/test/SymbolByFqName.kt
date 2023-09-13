/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path

object SymbolByFqName {
    fun getSymbolDataFromFile(filePath: Path): SymbolData {
        val testFileText = FileUtil.loadFile(filePath.toFile())
        val identifier = testFileText.lineSequence().first { line ->
            SymbolData.identifiers.any { identifier ->
                line.startsWith(identifier) || line.startsWith("// $identifier")
            }
        }
        return SymbolData.create(identifier.removePrefix("// "))
    }

    fun textWithRenderedSymbolData(filePath: String, rendered: String): String = buildString {
        val testFileText = FileUtil.loadFile(File(filePath))
        val fileTextWithoutSymbolsData = testFileText.substringBeforeLast(SYMBOLS_TAG).trimEnd()
        appendLine(fileTextWithoutSymbolsData)
        appendLine()
        appendLine(SYMBOLS_TAG)
        append(rendered)
    }


    private const val SYMBOLS_TAG = "// SYMBOLS:"
}

sealed class SymbolData {
    abstract fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol>

    data class ClassData(val classId: ClassId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol> {
            val symbol = getClassOrObjectSymbolByClassId(classId) ?: error("Class $classId is not found")
            return listOf(symbol)
        }
    }

    object ScriptData : SymbolData() {
        override fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol> {
            val script = ktFile.script ?: error("KtScript is not found")
            return listOf(script.getScriptSymbol())
        }
    }

    data class TypeAliasData(val classId: ClassId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol> {
            val symbol = getTypeAliasByClassId(classId) ?: error("Type alias $classId is not found")
            return listOf(symbol)
        }
    }

    data class CallableData(val callableId: CallableId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol> {
            val classId = callableId.classId
            val symbols = if (classId == null) {
                getTopLevelCallableSymbols(callableId.packageName, callableId.callableName).toList()
            } else {
                val classSymbol =
                    getClassOrObjectSymbolByClassId(classId)
                        ?: error("Class $classId is not found")
                classSymbol.getCombinedDeclaredMemberScope().getCallableSymbols(callableId.callableName)
                    .toList()
            }
            if (symbols.isEmpty()) {
                error("No callable with fqName $callableId found")
            }
            return symbols
        }
    }

    data class EnumEntryInitializerData(val enumEntryId: CallableId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(ktFile: KtFile): List<KtSymbol> {
            val classSymbol = enumEntryId.classId?.let { getClassOrObjectSymbolByClassId(it) }
                ?: error("Cannot find enum class `${enumEntryId.classId}`.")

            require(classSymbol is KtNamedClassOrObjectSymbol) { "`${enumEntryId.classId}` must be a named class." }
            require(classSymbol.classKind == KtClassKind.ENUM_CLASS) { "`${enumEntryId.classId}` must be an enum class." }

            val enumEntrySymbol = classSymbol.getEnumEntries().find { it.name == enumEntryId.callableName }
                ?: error("Cannot find enum entry symbol `$enumEntryId`.")

            val initializerSymbol = enumEntrySymbol.enumEntryInitializer ?: error("`${enumEntryId.callableName}` must have an initializer.")
            return listOf(initializerSymbol)
        }
    }

    companion object {
        val identifiers = arrayOf("callable:", "class:", "typealias:", "enum_entry_initializer:", "script")

        fun create(data: String): SymbolData = when {
            data == "script" -> ScriptData
            data.startsWith("class:") -> ClassData(ClassId.fromString(data.removePrefix("class:").trim()))
            data.startsWith("typealias:") -> TypeAliasData(ClassId.fromString(data.removePrefix("typealias:").trim()))
            data.startsWith("callable:") -> CallableData(extractCallableId(data, "callable:"))
            data.startsWith("enum_entry_initializer") -> EnumEntryInitializerData(extractCallableId(data, "enum_entry_initializer:"))
            else -> error("Invalid symbol kind, expected one of: $identifiers")
        }
    }
}

private fun extractCallableId(data: String, prefix: String): CallableId {
    val fullName = data.removePrefix(prefix).trim()
    val name = if ('.' in fullName) fullName.substringAfterLast(".") else fullName.substringAfterLast('/')
    val (packageName, className) = run {
        val packageNameWithClassName = fullName.dropLast(name.length + 1)
        when {
            '.' in fullName ->
                packageNameWithClassName.substringBeforeLast('/') to packageNameWithClassName.substringAfterLast('/')
            else -> packageNameWithClassName to null
        }
    }
    return CallableId(FqName(packageName.replace('/', '.')), className?.let { FqName(it) }, Name.identifier(name))
}
