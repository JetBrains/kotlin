/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.bir.generator.Packages.declarations
import org.jetbrains.kotlin.bir.generator.Packages.symbols
import org.jetbrains.kotlin.bir.generator.Packages.tree
import org.jetbrains.kotlin.bir.generator.Packages.types
import org.jetbrains.kotlin.bir.generator.Packages.visitors
import org.jetbrains.kotlin.generators.tree.TypeKind
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
object SymbolTypes {
    val packageFragment = type(symbols, "BirPackageFragmentSymbol")
    val file = type(symbols, "BirFileSymbol")
    val externalPackageFragment = type(symbols, "BirExternalPackageFragmentSymbol")
    val anonymousInitializer = type(symbols, "BirAnonymousInitializerSymbol")
    val enumEntry = type(symbols, "BirEnumEntrySymbol")
    val field = type(symbols, "BirFieldSymbol")
    val classifier = type(symbols, "BirClassifierSymbol")
    val `class` = type(symbols, "BirClassSymbol")
    val script = type(symbols, "BirScriptSymbol")
    val typeParameter = type(symbols, "BirTypeParameterSymbol")
    val value = type(symbols, "BirValueSymbol")
    val valueParameter = type(symbols, "BirValueParameterSymbol")
    val variable = type(symbols, "BirVariableSymbol")
    val returnTarget = type(symbols, "BirReturnTargetSymbol")
    val function = type(symbols, "BirFunctionSymbol")
    val constructor = type(symbols, "BirConstructorSymbol")
    val simpleFunction = type(symbols, "BirSimpleFunctionSymbol")
    val returnableBlock = type(symbols, "BirReturnableBlockSymbol")
    val property = type(symbols, "BirPropertySymbol")
    val localDelegatedProperty = type(symbols, "BirLocalDelegatedPropertySymbol")
    val typeAlias = type(symbols, "BirTypeAliasSymbol")
}