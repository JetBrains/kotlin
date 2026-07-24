/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaSymbolTestSymbolTargetResolver(private val session: KaSession) : TestSymbolTargetResolver<KaSymbol>() {
    override fun resolvePackageTarget(target: PackageTarget): List<KaSymbol> = with(session) {
        val symbol = findPackage(target.packageFqName) ?: error("Cannot find a symbol for the package `${target.packageFqName}`.")
        listOf(symbol)
    }

    override fun resolveClassTarget(target: ClassTarget): List<KaSymbol> = with(session) {
        val symbol = resolveClass(target.classId)
        listOf(symbol)
    }

    context(_: KaSession)
    private fun resolveClass(classId: ClassId): KaClassSymbol =
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
        val symbols = findCallables(target.callableId)
        symbols.ifEmpty { error("Cannot find a symbol for the callable `${target.callableId}`.") }
    }

    override fun resolvePropertyTarget(target: PropertyTarget): List<KaSymbol> = with(session) {
        val properties = findCallables(target.callableId).filterIsInstance<KaPropertySymbol>()
        properties.ifEmpty { error("Cannot find a property for `${target.callableId}`.") }
    }

    override fun resolveFunctionTarget(target: FunctionTarget): List<KaSymbol> = with(session) {
        var functions = findCallables(target.callableId).filterIsInstance<KaNamedFunctionSymbol>()
        if (target.parameterNames != null) {
            functions = functions.filter { it.matchesParameterNames(target.parameterNames) }
        }
        functions.ifEmpty { error("Cannot find a function for `$target`.") }
    }

    override fun resolveConstructorTarget(target: ConstructorTarget): List<KaSymbol> = with(session) {
        val classSymbol = resolveClass(target.classId)
        var constructors = classSymbol.declaredMemberScope.constructors.toList()
        if (target.parameterNames != null) {
            constructors = constructors.filter { it.matchesParameterNames(target.parameterNames) }
        }
        constructors.ifEmpty { error("Cannot find a constructor for `$target`.") }
    }

    context(_: KaSession)
    private fun findCallables(callableId: CallableId): List<KaCallableSymbol> {
        val classId = callableId.classId
        return if (classId == null) {
            findTopLevelCallables(callableId.packageName, callableId.callableName).toList()
        } else {
            val classSymbol = resolveClass(classId)
            findMatchingCallableSymbols(callableId, classSymbol)
        }
    }

    private fun KaFunctionSymbol.matchesParameterNames(expected: List<Name>): Boolean {
        return valueParameters.map { it.name } == expected
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

        val initializerSymbol = enumEntrySymbol.initializer ?: error("`${enumEntryId.callableName}` must have an initializer.")
        listOf(initializerSymbol)
    }

    override fun resolveSamConstructorTarget(target: SamConstructorTarget): List<KaSymbol> = with(session) {
        val symbol = findClassLike(target.classId) ?: error("Cannot find a symbol for the class `${target.classId}`.")
        val samConstructor = symbol.samConstructor ?: error("Cannot find a symbol for the SAM constructor of `${target.classId}`.")
        listOf(samConstructor)
    }

    override fun resolveTypeParameterTarget(target: TypeParameterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaDeclarationSymbol>(target, owner)
        return owner.typeParameters.find { it.name == target.name }
    }

    override fun resolveValueParameterTarget(target: ValueParameterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaFunctionSymbol>(target, owner)
        return owner.valueParameters.find { it.name == target.name }
    }

    override fun resolveContextParameterTarget(target: ContextParameterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaCallableSymbol>(target, owner)
        return owner.contextParameters.find { it.name == target.name }
    }

    override fun resolveGetterTarget(target: GetterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaPropertySymbol>(target, owner)
        return owner.getter
    }

    override fun resolveSetterTarget(target: SetterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaPropertySymbol>(target, owner)
        return owner.setter
    }

    override fun resolveReceiverParameterTarget(target: ReceiverParameterTarget, owner: KaSymbol): KaSymbol? {
        requireSpecificOwner<KaCallableSymbol>(target, owner)
        return owner.receiverParameter
    }

    override fun resolveFieldTarget(target: FieldTarget): KaSymbol? {
        val callables = resolveCallableTarget(CallableTarget(target.callableId))
        return callables
            .filterIsInstance<KaPropertySymbol>()
            .singleOrNull()
            ?.backingFieldSymbol
    }
}
