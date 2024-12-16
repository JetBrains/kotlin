/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.symbols

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.symbols.SymbolByFqName.getSymbolDataFromFile
import org.jetbrains.kotlin.analysis.test.framework.symbols.SymbolData.SymbolDataWithOwner.TypeParameterData
import org.jetbrains.kotlin.analysis.test.framework.symbols.SymbolData.SymbolDataWithOwner.ValueParameterData
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
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

inline fun <reified S : KaSymbol> KaSession.getSingleTestTargetSymbolOfType(mainFile: KtFile, testDataPath: Path): S {
    val symbols = with(getSymbolDataFromFile(testDataPath)) { toSymbols(mainFile) }
    return symbols.singleOrNull() as? S
        ?: error("Expected a single target `${S::class.simpleName}` to be specified, but found the following symbols: $symbols")
}

sealed class SymbolData {
    abstract fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol>

    data class PackageData(val packageFqName: FqName) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val symbol = findPackage(packageFqName) ?: error("Cannot find a symbol for the package `$packageFqName`.")
            return listOf(symbol)
        }
    }

    data class ClassData(val classId: ClassId) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val symbol = findClass(classId) ?: error("Class $classId is not found")
            return listOf(symbol)
        }
    }

    object ScriptData : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val script = ktFile.script ?: error("Script is not found")
            return listOf(script.symbol)
        }
    }

    data class TypeAliasData(val classId: ClassId) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val symbol = findTypeAlias(classId) ?: error("Type alias $classId is not found")
            return listOf(symbol)
        }
    }

    data class CallableData(val callableId: CallableId) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val classId = callableId.classId

            val symbols = if (classId == null) {
                findTopLevelCallables(callableId.packageName, callableId.callableName).toList()
            } else {
                val classSymbol = findClass(classId) ?: error("Class $classId is not found")
                findMatchingCallableSymbols(classSymbol)
            }

            if (symbols.isEmpty()) {
                error("No callable with fqName $callableId found")
            }

            return symbols
        }

        private fun KaSession.findMatchingCallableSymbols(classSymbol: KaClassSymbol): List<KaCallableSymbol> {
            val declaredSymbols = classSymbol.combinedDeclaredMemberScope
                .callables(callableId.callableName).toList()

            if (declaredSymbols.isNotEmpty()) {
                return declaredSymbols
            }

            // Fake overrides are absent in the declared member scope
            return classSymbol.combinedMemberScope
                .callables(callableId.callableName)
                .filter { it.containingDeclaration == classSymbol }
                .toList()
        }
    }

    data class EnumEntryInitializerData(val enumEntryId: CallableId) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val classSymbol = enumEntryId.classId?.let { findClass(it) }
                ?: error("Cannot find enum class `${enumEntryId.classId}`.")

            require(classSymbol is KaNamedClassSymbol) { "`${enumEntryId.classId}` must be a named class." }
            require(classSymbol.classKind == KaClassKind.ENUM_CLASS) { "`${enumEntryId.classId}` must be an enum class." }

            val enumEntrySymbol = classSymbol.staticDeclaredMemberScope
                .callables(enumEntryId.callableName)
                .filterIsInstance<KaEnumEntrySymbol>().find {
                    it.name == enumEntryId.callableName
                }
                ?: error("Cannot find enum entry symbol `$enumEntryId`.")

            val initializerSymbol = enumEntrySymbol.enumEntryInitializer ?: error("`${enumEntryId.callableName}` must have an initializer.")
            return listOf(initializerSymbol)
        }
    }

    data class SamConstructorData(val classId: ClassId) : SymbolData() {
        override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val symbol = findClassLike(classId) ?: error("Class-like symbol is not found by '$classId'")
            val samConstructor = symbol.samConstructor ?: error("SAM constructor is not found for symbol '$symbol'")
            return listOf(samConstructor)
        }
    }

    sealed class SymbolDataWithOwner : SymbolData() {
        abstract val ownerData: SymbolData

        final override fun KaSession.toSymbols(ktFile: KtFile): List<KaSymbol> {
            val owner = with(ownerData) { toSymbols(ktFile) }.singleOrNull() ?: error("No owner found")
            return toMemberSymbols(owner)
        }

        abstract fun KaSession.toMemberSymbols(owner: KaSymbol): List<KaSymbol>

        data class TypeParameterData(val name: Name, override val ownerData: SymbolData) : SymbolDataWithOwner() {
            override fun KaSession.toMemberSymbols(owner: KaSymbol): List<KaSymbol> {
                requireIsInstance<KaDeclarationSymbol>(owner)
                val parameterSymbol = owner.typeParameters.find { it.name == name }
                    ?: error("Type parameter with '$name' name is not found in $ownerData")

                return listOf(parameterSymbol)
            }
        }

        data class ValueParameterData(val name: Name, override val ownerData: SymbolData) : SymbolDataWithOwner() {
            override fun KaSession.toMemberSymbols(owner: KaSymbol): List<KaSymbol> {
                requireIsInstance<KaFunctionSymbol>(owner)
                val parameterSymbol = owner.valueParameters.find { it.name == name }
                    ?: error("Value parameter with '$name' name is not found in $ownerData")

                return listOf(parameterSymbol)
            }
        }
    }


    companion object {
        val identifiers = arrayOf(
            "package:",
            "callable:",
            "class:",
            "typealias:",
            "enum_entry_initializer:",
            "script",
            "sam_constructor:",
            "type_parameter:",
            "value_parameter:",
        )

        fun create(data: String): SymbolData {
            val key = data.substringBefore(":")
            val value = data.substringAfter(":").trim()
            return when (key) {
                "script" -> ScriptData
                "package" -> PackageData(extractPackageFqName(value))
                "class" -> ClassData(ClassId.fromString(value))
                "typealias" -> TypeAliasData(ClassId.fromString(value))
                "callable" -> CallableData(extractCallableId(value))
                "enum_entry_initializer" -> EnumEntryInitializerData(extractCallableId(value))
                "sam_constructor" -> SamConstructorData(ClassId.fromString(value))
                "type_parameter" -> extractTypeParameterData(value)
                "value_parameter" -> extractValueParameterData(value)
                else -> error("Invalid symbol kind, expected one of: $identifiers")
            }
        }
    }
}

private fun extractOwnerData(data: String): Pair<String, SymbolData> {
    val customData = data.substringBefore(":")
    val owner = data.substringAfter(":").trim()
    val ownerData = SymbolData.create(owner)
    return customData to ownerData
}

private fun extractTypeParameterData(data: String): TypeParameterData {
    val (typeParameterName, ownerData) = extractOwnerData(data)
    return TypeParameterData(Name.identifier(typeParameterName), ownerData)
}

private fun extractValueParameterData(data: String): ValueParameterData {
    val (valueParameterName, ownerData) = extractOwnerData(data)
    return ValueParameterData(Name.identifier(valueParameterName), ownerData)
}

private fun extractPackageFqName(data: String): FqName = FqName.fromSegments(data.split('.'))

private fun extractCallableId(fullName: String): CallableId {
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
