/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

class MissingFakeOverridesAdder(
    private val expectActualMap: Map<IrSymbol, IrSymbol>,
    private val typeAliasMap: Map<FqName, FqName>,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext
) : IrElementVisitorVoid {
    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect) {
            processSupertypes(declaration)
        }
        visitElement(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun processSupertypes(declaration: IrClass) {
        val members by lazy(LazyThreadSafetyMode.NONE) {
            val notBuiltinMembers = declaration.declarations.filterNot { it.isBuiltinMember() }
            val result = mutableMapOf<String, MutableList<IrDeclaration>>()
            for (member in notBuiltinMembers) {
                result.getOrPut(generateIrElementFullNameFromExpect(member, typeAliasMap)) { mutableListOf() }.add(member)
            }
            result
        }

        for (superType in declaration.superTypes) {
            val expectClass = superType.classifierOrFail.owner as? IrClass ?: continue
            val added = mutableSetOf<IrDeclaration>()
            for (expectMember in expectClass.declarations) {
                if (expectMember.isBuiltinMember()) continue
                val actualMember = expectActualMap[expectMember.symbol]?.owner as? IrDeclaration ?: continue
                added += actualMember

                // Do not add FAKE_OVERRIDE if the subclass already has overridden member
                if (declaration.declarations.filterIsInstance<IrOverridableDeclaration<*>>()
                        .any { expectMember.symbol in it.overriddenSymbols }
                ) {
                    continue
                }
                addFakeOverride(actualMember, members, declaration)
            }
            val actualClass = expectActualMap[expectClass.symbol]?.owner as? IrClass ?: continue
            for (actualMember in actualClass.declarations) {
                if (actualMember.isBuiltinMember() || actualMember in added) continue
                addFakeOverride(actualMember, members, declaration)
            }
        }
    }

    private fun addFakeOverride(
        actualMember: IrDeclaration,
        members: MutableMap<String, MutableList<IrDeclaration>>,
        declaration: IrClass
    ) {
        val newMember = when (actualMember) {
            is IrFunctionImpl -> createFakeOverrideFunction(actualMember, declaration)
            is IrPropertyImpl -> createFakeOverrideProperty(actualMember, declaration)
            else -> return
        }

        if (members.getMatches(newMember, expectActualMap, typeAliasMap).isEmpty()) {
            declaration.declarations.add(newMember)
            members.getOrPut(generateIrElementFullNameFromExpect(newMember, typeAliasMap)) { mutableListOf() }.add(newMember)
        } else {
            diagnosticsReporter.at(declaration).report(
                CommonBackendErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED,
                declaration.name.asString(),
                (actualMember as IrDeclarationWithName).name.asString()
            )
        }
    }
}

private fun IrDeclaration.isBuiltinMember(): Boolean {
    if (this !is IrFunction) return false
    return this is IrConstructor || dispatchReceiverParameter?.type?.isAny() == true
}

private fun createFakeOverrideProperty(actualMember: IrPropertyImpl, declaration: IrClass) =
    declaration.factory.buildProperty {
        updateFrom(actualMember)
        name = actualMember.name
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
    }.apply {
        parent = declaration
        annotations = actualMember.annotations
        backingField = actualMember.backingField
        getter = (actualMember.getter as? IrFunctionImpl)?.let { getter ->
            createFakeOverrideFunction(getter, declaration, symbol)
        }
        setter = (actualMember.setter as? IrFunctionImpl)?.let { setter ->
            createFakeOverrideFunction(setter, declaration, symbol)
        }
        overriddenSymbols = listOf(actualMember.symbol)
    }

private fun createFakeOverrideFunction(
    actualFunction: IrFunctionImpl,
    parent: IrDeclarationParent,
    correspondingPropertySymbol: IrPropertySymbol? = null
) = actualFunction.factory.buildFun {
    updateFrom(actualFunction)
    name = actualFunction.name
    returnType = actualFunction.returnType
    origin = IrDeclarationOrigin.FAKE_OVERRIDE
    isFakeOverride = true
    isExpect = false
}.also {
    it.parent = parent
    it.annotations = actualFunction.annotations.map { p -> p.deepCopyWithSymbols(it) }
    it.typeParameters = actualFunction.typeParameters.map { p -> p.deepCopyWithSymbols(it) }
    it.dispatchReceiverParameter = actualFunction.dispatchReceiverParameter?.deepCopyWithSymbols(it)
    it.extensionReceiverParameter = actualFunction.extensionReceiverParameter?.deepCopyWithSymbols(it)
    it.valueParameters = actualFunction.valueParameters.map { p -> p.deepCopyWithSymbols(it) }
    it.contextReceiverParametersCount = actualFunction.contextReceiverParametersCount
    it.metadata = actualFunction.metadata
    it.overriddenSymbols = listOf(actualFunction.symbol)
    it.attributeOwnerId = it
    it.correspondingPropertySymbol = correspondingPropertySymbol
}
