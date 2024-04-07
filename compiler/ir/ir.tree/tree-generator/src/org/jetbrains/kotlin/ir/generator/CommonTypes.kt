/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.ir.generator.Packages.declarations
import org.jetbrains.kotlin.ir.generator.Packages.exprs
import org.jetbrains.kotlin.ir.generator.Packages.symbols
import org.jetbrains.kotlin.ir.generator.Packages.tree
import org.jetbrains.kotlin.ir.generator.Packages.types
import org.jetbrains.kotlin.ir.generator.Packages.util
import org.jetbrains.kotlin.ir.generator.Packages.visitors

object Packages {
    const val tree = "org.jetbrains.kotlin.ir"
    const val exprs = "org.jetbrains.kotlin.ir.expressions"
    const val symbols = "org.jetbrains.kotlin.ir.symbols"
    const val declarations = "org.jetbrains.kotlin.ir.declarations"
    const val types = "org.jetbrains.kotlin.ir.types"
    const val visitors = "org.jetbrains.kotlin.ir.visitors"
    const val descriptors = "org.jetbrains.kotlin.descriptors"
    const val util = "org.jetbrains.kotlin.ir.util"
}

val elementBaseType = type(tree, "IrElementBase", TypeKind.Class)
val statementOriginType = type(exprs, "IrStatementOrigin")
val elementVisitorType = type(visitors, "IrElementVisitor")
val elementVisitorVoidType = type(visitors, "IrElementVisitorVoid")
val elementTransformerType = type(visitors, "IrElementTransformer")
val elementTransformerVoidType = type(visitors, "IrElementTransformerVoid", TypeKind.Class)
val typeTransformerType = type(visitors, "IrTypeTransformer")
val irTypeType = type(types, "IrType")
val irFactoryType = type(declarations, "IrFactory")
val stageControllerType = type(declarations, "StageController", TypeKind.Class)
val idSignatureType = type(util, "IdSignature", TypeKind.Class)
val irImplementationDetailType = type(tree, "IrImplementationDetail", TypeKind.Class)

val symbolType = type(symbols, "IrSymbol")
val packageFragmentSymbolType = type(symbols, "IrPackageFragmentSymbol")
val fileSymbolType = type(symbols, "IrFileSymbol")
val externalPackageFragmentSymbolType = type(symbols, "IrExternalPackageFragmentSymbol")
val anonymousInitializerSymbolType = type(symbols, "IrAnonymousInitializerSymbol")
val enumEntrySymbolType = type(symbols, "IrEnumEntrySymbol")
val fieldSymbolType = type(symbols, "IrFieldSymbol")
val classifierSymbolType = type(symbols, "IrClassifierSymbol")
val classSymbolType = type(symbols, "IrClassSymbol")
val scriptSymbolType = type(symbols, "IrScriptSymbol")
val typeParameterSymbolType = type(symbols, "IrTypeParameterSymbol")
val valueSymbolType = type(symbols, "IrValueSymbol")
val valueParameterSymbolType = type(symbols, "IrValueParameterSymbol")
val variableSymbolType = type(symbols, "IrVariableSymbol")
val returnTargetSymbolType = type(symbols, "IrReturnTargetSymbol")
val functionSymbolType = type(symbols, "IrFunctionSymbol")
val constructorSymbolType = type(symbols, "IrConstructorSymbol")
val simpleFunctionSymbolType = type(symbols, "IrSimpleFunctionSymbol")
val returnableBlockSymbolType = type(symbols, "IrReturnableBlockSymbol")
val propertySymbolType = type(symbols, "IrPropertySymbol")
val localDelegatedPropertySymbolType = type(symbols, "IrLocalDelegatedPropertySymbol")
val typeAliasSymbolType = type(symbols, "IrTypeAliasSymbol")

val obsoleteDescriptorBasedApiAnnotation = type(BASE_PACKAGE, "ObsoleteDescriptorBasedAPI", TypeKind.Class)
val unsafeDuringIrConstructionApiAnnotation = type(symbols, "UnsafeDuringIrConstructionAPI", TypeKind.Class)
