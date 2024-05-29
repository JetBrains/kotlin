/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

object IrPackages {
    const val tree = "org.jetbrains.kotlin.ir"
    const val exprs = "org.jetbrains.kotlin.ir.expressions"
    const val symbols = "org.jetbrains.kotlin.ir.symbols"
    const val symbolsImpl = "org.jetbrains.kotlin.ir.symbols.impl"
    const val declarations = "org.jetbrains.kotlin.ir.declarations"
    const val types = "org.jetbrains.kotlin.ir.types"
    const val visitors = "org.jetbrains.kotlin.ir.visitors"
    const val descriptors = "org.jetbrains.kotlin.descriptors"
    const val util = "org.jetbrains.kotlin.ir.util"
}

object Packages {
    const val tree = "org.jetbrains.kotlin.bir"
    const val symbols = "org.jetbrains.kotlin.bir.symbols"
    const val symbolsImpl = "org.jetbrains.kotlin.bir.symbols.impl"
    const val declarations = "org.jetbrains.kotlin.bir.declarations"
    const val expressions = "org.jetbrains.kotlin.bir.expressions"
    const val types = "org.jetbrains.kotlin.bir.types"
    const val visitors = "org.jetbrains.kotlin.bir.visitors"
    const val util = "org.jetbrains.kotlin.bir.util"
    const val descriptors = "org.jetbrains.kotlin.descriptors"
}

val elementBaseType = type(tree, "BirElementBase", TypeKind.Class)
val elementImplBaseType = type(tree, "BirImplElementBase", TypeKind.Class)
val mutableAnnotationContainerType = type(declarations, "BirMutableAnnotationContainer")
val childElementList = type(tree, "BirChildElementList")
val elementClassType = type(tree, "BirElementClass")

val elementVisitorType = type(tree, "BirElementVisitor")
val elementVisitorVoidType = type(visitors, "BirElementVisitorVoid")
val elementTransformerType = type(visitors, "BirElementTransformer")
val elementTransformerVoidType = type(visitors, "BirElementTransformerVoid", TypeKind.Class)

val irTypeType = type(types, "BirType")
val irSimpleTypeType = type(types, "BirSimpleType", TypeKind.Class)
val irTypeAbbreviationType = type(types, "BirTypeAbbreviation")

val symbolType = type(symbols, "BirSymbol")
val idSignatureType = type(IrPackages.util, "IdSignature", TypeKind.Class)
val statementOriginType = type(IrPackages.exprs, "IrStatementOrigin")
val stageControllerType = type(IrPackages.declarations, "StageController", TypeKind.Class)

val birImplementationDetailType = type(Packages.util, "BirImplementationDetail", TypeKind.Class)
val irImplementationDetailType = type(IrPackages.util, "IrImplementationDetail", TypeKind.Class)
val irElementConstructorIndicatorType = type(IrPackages.util, "IrElementConstructorIndicator", TypeKind.Class)
val unsafeDuringIrConstructionApiAnnotation = type(IrPackages.symbols, "UnsafeDuringIrConstructionAPI", TypeKind.Class)
val obsoleteDescriptorBasedApiAnnotation = type(IrPackages.tree, "ObsoleteDescriptorBasedAPI", TypeKind.Class)

/*
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
}*/
