/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class MissingFakeOverridesAdder(private val expectActualMap: Map<IrSymbol, IrSymbol>) : IrElementVisitorVoid {
    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect) {
            processSupertypes(declaration, expectActualMap)
        }
        visitElement(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}

private fun processSupertypes(declaration: IrClass, expectActualMap: Map<IrSymbol, IrSymbol>) {
    val members by lazy(LazyThreadSafetyMode.NONE) {
        declaration.declarations.filter { !it.isBuiltinMember() }.filterIsInstance<IrDeclarationWithName>()
            .groupBy { it.name }
    }

    for (superType in declaration.superTypes) {
        val actualClass = expectActualMap[superType.classifierOrFail]?.owner as? IrClass ?: continue
        for (actualMember in actualClass.declarations) {
            if (actualMember.isBuiltinMember()) continue
            when (actualMember) {
                is IrFunctionImpl -> {
                    val existingMembers = members[actualMember.name]

                    var isActualFunctionFound = false
                    if (existingMembers != null) {
                        for (existingMember in existingMembers) {
                            if (existingMember is IrFunction) {
                                if (checkParameters(existingMember, actualMember, expectActualMap)) {
                                    isActualFunctionFound = true
                                    break
                                }
                            }
                        }
                    }

                    if (isActualFunctionFound) {
                        reportManyInterfacesMembersNotImplemented(declaration, actualMember)
                        continue
                    }

                    declaration.declarations.add(createFakeOverrideFunction(actualMember, declaration))
                }
                is IrPropertyImpl -> {
                    if (members[actualMember.name] != null) {
                        reportManyInterfacesMembersNotImplemented(declaration, actualMember)
                        continue
                    }

                    declaration.declarations.add(createFakeOverrideProperty(actualMember, declaration))
                }
            }
        }
    }
}

private fun IrDeclaration.isBuiltinMember(): Boolean {
    if (this !is IrFunction) return false
    return this is IrConstructor || dispatchReceiverParameter?.type?.isAny() == true
}

private fun createFakeOverrideProperty(actualMember: IrPropertyImpl, declaration: IrClass) =
    IrPropertyImpl(
        actualMember.startOffset,
        actualMember.endOffset,
        IrDeclarationOrigin.FAKE_OVERRIDE,
        IrPropertySymbolImpl(),
        actualMember.name,
        actualMember.visibility,
        actualMember.modality,
        actualMember.isVar,
        actualMember.isConst,
        actualMember.isLateinit,
        actualMember.isDelegated,
        isExternal = actualMember.isExternal
    ).also {
        it.parent = declaration
        it.annotations = actualMember.annotations
        it.backingField = actualMember.backingField
        it.getter = (actualMember.getter as? IrFunctionImpl)?.let { getter ->
            createFakeOverrideFunction(getter, declaration, it.symbol)
        }
        it.setter = (actualMember.setter as? IrFunctionImpl)?.let { setter ->
            createFakeOverrideFunction(setter, declaration, it.symbol)
        }
        it.overriddenSymbols = listOf(actualMember.symbol)
        it.metadata = actualMember.metadata
        it.attributeOwnerId = it
    }

private fun createFakeOverrideFunction(
    actualFunction: IrFunctionImpl,
    parent: IrDeclarationParent,
    correspondingPropertySymbol: IrPropertySymbol? = null
) =
    IrFunctionImpl(
        actualFunction.startOffset,
        actualFunction.endOffset,
        IrDeclarationOrigin.FAKE_OVERRIDE,
        IrSimpleFunctionSymbolImpl(),
        actualFunction.name,
        actualFunction.visibility,
        actualFunction.modality,
        actualFunction.returnType,
        actualFunction.isInline,
        actualFunction.isExternal,
        actualFunction.isTailrec,
        actualFunction.isSuspend,
        actualFunction.isOperator,
        actualFunction.isInfix,
        isExpect = false
    ).also {
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