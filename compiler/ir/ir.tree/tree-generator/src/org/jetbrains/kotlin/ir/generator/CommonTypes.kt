/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.ir.generator.Packages.declarations
import org.jetbrains.kotlin.ir.generator.Packages.descriptors
import org.jetbrains.kotlin.ir.generator.Packages.exprs
import org.jetbrains.kotlin.ir.generator.Packages.symbols
import org.jetbrains.kotlin.ir.generator.Packages.symbolsImpl
import org.jetbrains.kotlin.ir.generator.Packages.tree
import org.jetbrains.kotlin.ir.generator.Packages.types
import org.jetbrains.kotlin.ir.generator.Packages.util
import org.jetbrains.kotlin.ir.generator.Packages.visitors

object Packages {
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

val elementBaseType = type(tree, "IrElementBase", TypeKind.Class)
val statementOriginType = type(exprs, "IrStatementOrigin")
val elementVisitorType = type(visitors, "IrElementVisitor")
val elementVisitorVoidType = type(visitors, "IrElementVisitorVoid")
val elementTransformerType = type(visitors, "IrElementTransformer")
val elementTransformerVoidType = type(visitors, "IrElementTransformerVoid", TypeKind.Class)
val typeTransformerType = type(visitors, "IrTypeTransformer")
val typeTransformerVoidType = type(visitors, "IrTypeTransformerVoid", TypeKind.Class)
val irTypeType = type(types, "IrType")
val irSimpleTypeType = type(types, "IrSimpleType", TypeKind.Class)
val irTypeAbbreviationType = type(types, "IrTypeAbbreviation")
val irFactoryType = type(declarations, "IrFactory")
val stageControllerType = type(declarations, "StageController", TypeKind.Class)
val idSignatureType = type(util, "IdSignature", TypeKind.Class)
val symbolRemapperType = type(util, "SymbolRemapper")
val declaredSymbolRemapperType = type(util, "DeclaredSymbolRemapper")
val referencedSymbolRemapperType = type(util, "ReferencedSymbolRemapper")
val emptySymbolRemapperType = ClassRef<PositionTypeParameterRef>(TypeKind.Class, util, "SymbolRemapper", "Empty")
val irImplementationDetailType = type(tree, "IrImplementationDetail", TypeKind.Class)
val irElementConstructorIndicatorType = type(util, "IrElementConstructorIndicator", TypeKind.Class)

val irSymbolBaseType = type(symbolsImpl, "IrSymbolBase", TypeKind.Class)
val irSymbolWithSignatureType = type(symbolsImpl, "IrSymbolWithSignature", TypeKind.Class)

val obsoleteDescriptorBasedApiAnnotation = type(BASE_PACKAGE, "ObsoleteDescriptorBasedAPI", TypeKind.Class)
val unsafeDuringIrConstructionApiAnnotation = type(symbols, "UnsafeDuringIrConstructionAPI", TypeKind.Class)
