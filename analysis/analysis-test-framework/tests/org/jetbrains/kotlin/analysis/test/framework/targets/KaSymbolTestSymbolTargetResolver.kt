/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.*
import org.jetbrains.kotlin.name.ClassId

internal class KaSymbolTestSymbolTargetResolver(private val session: KaSession) : TestSymbolTargetResolver<KaSymbol>() {
    override fun resolvePackageTarget(target: PackageTarget): List<KaSymbol> = with(session) {
        val symbol = findPackage(target.packageFqName) ?: error("Cannot find a symbol for the package `${target.packageFqName}`.")
        listOf(symbol)
    }

    override fun resolveClassTarget(target: ClassTarget): List<KaSymbol> = with(session) {
        val symbol = resolveClass(target.classId)
        listOf(symbol)
    }

    private fun KaSession.resolveClass(classId: ClassId): KaClassSymbol =
        findClass(classId) ?: error("Cannot find a symbol for the class `$classId`.")

    override fun resolveScriptTarget(target: ScriptTarget): List<KaSymbol> = with(session) {
        val script = target.file.script ?: error("The file `${target.file.name}` is not a script.")
        listOf(script.symbol)
    }

    override fun resolveTypeAliasTarget(target: TypeAliasTarget): List<KaSymbol> = with(session) {
        val symbol = findTypeAlias(target.classId) ?: error("Cannot find a symbol for the type alias `${target.classId}`.")
        listOf(symbol)
    }

    override fun resolveCallableTarget(target: CallableTarget): List<KaSymbol> = with(session) {
        val callableId = target.callableId
        val classId = callableId.classId

        val symbols = if (classId == null) {
            findTopLevelCallables(callableId.packageName, callableId.callableName).toList()
        } else {
            val classSymbol = resolveClass(classId)
            findMatchingCallableSymbols(callableId, classSymbol)
        }

        if (symbols.isEmpty()) {
            error("Cannot find a symbol for the callable `$callableId`.")
        }

        symbols
    }

    override fun resolveEnumEntryInitializerTarget(target: EnumEntryInitializerTarget): List<KaSymbol> = with(session) {
        val enumEntryId = target.enumEntryId

        val classSymbol = enumEntryId.classId?.let { findClass(it) }
            ?: error("Cannot find a symbol for the enum class `${enumEntryId.classId}`.")

        require(classSymbol is KaNamedClassSymbol) { "`${enumEntryId.classId}` must be a named class." }
        require(classSymbol.classKind == KaClassKind.ENUM_CLASS) { "`${enumEntryId.classId}` must be an enum class." }

        val enumEntrySymbol = classSymbol.staticDeclaredMemberScope
            .callables(enumEntryId.callableName)
            .filterIsInstance<KaEnumEntrySymbol>().find {
                it.name == enumEntryId.callableName
            }
            ?: error("Cannot find a symbol for the enum entry `$enumEntryId`.")

        val initializerSymbol = enumEntrySymbol.enumEntryInitializer ?: error("`${enumEntryId.callableName}` must have an initializer.")
        listOf(initializerSymbol)
    }

    override fun resolveSamConstructorTarget(target: SamConstructorTarget): List<KaSymbol> = with(session) {
        val symbol = findClassLike(target.classId) ?: error("Cannot find a symbol for the class `${target.classId}`.")
        val samConstructor = symbol.samConstructor ?: error("Cannot find a symbol for the SAM constructor of `${target.classId}`.")
        listOf(samConstructor)
    }

    override fun resolveTypeParameterTarget(target: TypeParameterTarget, owner: KaSymbol): KaSymbol? = with(session) {
        requireSpecificOwner<KaTypeParameterOwnerSymbol>(target, owner)

        owner.typeParameters.find { it.name == target.name }
    }

    override fun resolveValueParameterTarget(target: ValueParameterTarget, owner: KaSymbol): KaSymbol? = with(session) {
        requireSpecificOwner<KaFunctionSymbol>(target, owner)

        owner.valueParameters.find { it.name == target.name }
    }
}
