/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.bir.generator.Packages.declarations
import org.jetbrains.kotlin.bir.generator.Packages.exprs
import org.jetbrains.kotlin.bir.generator.Packages.symbols
import org.jetbrains.kotlin.bir.generator.Packages.tree
import org.jetbrains.kotlin.bir.generator.Packages.types
import org.jetbrains.kotlin.bir.generator.Packages.visitors
import org.jetbrains.kotlin.generators.tree.type

object Packages {
    const val tree = "org.jetbrains.kotlin.bir"
    const val exprs = "org.jetbrains.kotlin.bir.expressions"
    const val symbols = "org.jetbrains.kotlin.bir.symbols"
    const val declarations = "org.jetbrains.kotlin.bir.declarations"
    const val types = "org.jetbrains.kotlin.bir.types"
    const val visitors = "org.jetbrains.kotlin.bir.visitors"
    const val descriptors = "org.jetbrains.kotlin.descriptors"
}

val elementBaseType = type(tree, "BirElementBase", TypeKind.Class)
val statementOriginType = type("org.jetbrains.kotlin.ir.expressions", "IrStatementOrigin")
val elementVisitorType = type(visitors, "BirElementVisitor")
val elementTransformerType = type(visitors, "BirElementTransformer")
val mutableAnnotationContainerType = type(declarations, "BirMutableAnnotationContainer")
val irTypeType = type(types, "BirType")

val symbolType = type(symbols, "BirSymbol")
val packageFragmentSymbolType = type(symbols, "BirPackageFragmentSymbol")
val fileSymbolType = type(symbols, "BirFileSymbol")
val externalPackageFragmentSymbolType = type(symbols, "BirExternalPackageFragmentSymbol")
val anonymousInitializerSymbolType = type(symbols, "BirAnonymousInitializerSymbol")
val enumEntrySymbolType = type(symbols, "BirEnumEntrySymbol")
val fieldSymbolType = type(symbols, "BirFieldSymbol")
val classifierSymbolType = type(symbols, "BirClassifierSymbol")
val classSymbolType = type(symbols, "BirClassSymbol")
val scriptSymbolType = type(symbols, "BirScriptSymbol")
val typeParameterSymbolType = type(symbols, "BirTypeParameterSymbol")
val valueSymbolType = type(symbols, "BirValueSymbol")
val valueParameterSymbolType = type(symbols, "BirValueParameterSymbol")
val variableSymbolType = type(symbols, "BirVariableSymbol")
val returnTargetSymbolType = type(symbols, "BirReturnTargetSymbol")
val functionSymbolType = type(symbols, "BirFunctionSymbol")
val constructorSymbolType = type(symbols, "BirConstructorSymbol")
val simpleFunctionSymbolType = type(symbols, "BirSimpleFunctionSymbol")
val returnableBlockSymbolType = type(symbols, "BirReturnableBlockSymbol")
val propertySymbolType = type(symbols, "BirPropertySymbol")
val localDelegatedPropertySymbolType = type(symbols, "BirLocalDelegatedPropertySymbol")
val typeAliasSymbolType = type(symbols, "BirTypeAliasSymbol")
