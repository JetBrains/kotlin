/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
    abstract fun KtAnalysisSession.toSymbols(): List<KtSymbol>

    data class ClassData(val classId: ClassId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(): List<KtSymbol> {
            val symbol = classId.getCorrespondingToplevelClassOrObjectSymbol() ?: error("Class $classId is not found")
            return listOf(symbol)
        }
    }

    data class CallableData(val callableId: CallableId) : SymbolData() {
        override fun KtAnalysisSession.toSymbols(): List<KtSymbol> {
            val classId = callableId.classId
            val symbols = if (classId == null) {
                callableId.packageName.getContainingCallableSymbolsWithName(callableId.callableName).toList()
            } else {
                val classSymbol =
                    classId.getCorrespondingToplevelClassOrObjectSymbol()
                        ?: error("Class $classId is not found")
                classSymbol.getDeclaredMemberScope().getCallableSymbols()
                    .filter { (it as? KtNamedSymbol)?.name == callableId.callableName }
                    .toList()
            }
            if (symbols.isEmpty()) {
                error("No callable with fqName $callableId found")
            }
            return symbols
        }
    }

    companion object {
        val identifiers = arrayOf("callable:", "class:")

        fun create(data: String): SymbolData = when {
            data.startsWith("class:") -> ClassData(ClassId.fromString(data.removePrefix("class:").trim()))
            data.startsWith("callable:") -> {
                val fullName = data.removePrefix("callable:").trim()
                val name = if ('.' in fullName) fullName.substringAfterLast(".") else fullName.substringAfterLast('/')
                val (packageName, className) = run {
                    val packageNameWithClassName = fullName.dropLast(name.length + 1)
                    when {
                        '.' in fullName ->
                            packageNameWithClassName.substringBeforeLast('/') to packageNameWithClassName.substringAfterLast('/')
                        else -> packageNameWithClassName to null
                    }
                }
                CallableData(CallableId(FqName(packageName.replace('/', '.')), className?.let { FqName(it) }, Name.identifier(name)))
            }
            else -> error("Invalid symbol")
        }
    }
}

