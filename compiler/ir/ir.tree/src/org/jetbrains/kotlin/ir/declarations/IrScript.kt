/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

//TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
//NOTE: declarations and statements stored separately
abstract class IrScript :
    IrDeclarationBase(), IrDeclarationWithName,
    IrDeclarationParent, IrStatementContainer, IrMetadataSourceOwner {

    abstract override val symbol: IrScriptSymbol

    // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
    // TODO: consider removing from here and handle appropriately in the lowering
    abstract var thisReceiver: IrValueParameter

    abstract var baseClass: IrType

    abstract var explicitCallParameters: List<IrValueParameter>

    abstract var implicitReceiversParameters: List<IrValueParameter>

    abstract var providedProperties: List<Pair<IrValueParameter, IrPropertySymbol>>

    abstract var resultProperty: IrPropertySymbol?

    abstract var earlierScriptsParameter: IrValueParameter?

    abstract var earlierScripts: List<IrScriptSymbol>?

    abstract var targetClass: IrClassSymbol?

    abstract var constructor: IrConstructor?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitScript(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super<IrStatementContainer>.acceptChildren(visitor, data)
        thisReceiver.accept(visitor, data)
        explicitCallParameters.forEach { it.accept(visitor, data) }
        implicitReceiversParameters.forEach { it.accept(visitor, data) }
        providedProperties.forEach { it.first.accept(visitor, data) }
        earlierScriptsParameter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        super<IrStatementContainer>.transformChildren(transformer, data)
        thisReceiver = thisReceiver.transform(transformer, data)
        explicitCallParameters = explicitCallParameters.map { it.transform(transformer, data) }
        implicitReceiversParameters = implicitReceiversParameters.map { it.transform(transformer, data) }
        providedProperties = providedProperties.map { it.first.transform(transformer, data) to it.second }
        earlierScriptsParameter = earlierScriptsParameter?.transform(transformer, data)
    }
}
