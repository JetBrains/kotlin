/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.SCRIPT_ORIGIN
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.script]
 */
abstract class IrScript(
    startOffset: Int,
    endOffset: Int,
    override val factory: IrFactory,
    override var name: Name,
    override val symbol: IrScriptSymbol,
) : IrDeclarationBase(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = SCRIPT_ORIGIN,
), IrDeclarationWithName, IrDeclarationParent, IrStatementContainer, IrMetadataSourceOwner {
    override var annotations: List<IrConstructorCall> = emptyList()

    override val statements: MutableList<IrStatement> = ArrayList(2)

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ScriptDescriptor

    var thisReceiver: IrValueParameter? = null

    var baseClass: IrType? = null

    lateinit var explicitCallParameters: List<IrVariable>

    lateinit var implicitReceiversParameters: List<IrValueParameter>

    lateinit var providedProperties: List<IrPropertySymbol>

    lateinit var providedPropertiesParameters: List<IrValueParameter>

    var resultProperty: IrPropertySymbol? = null

    var earlierScriptsParameter: IrValueParameter? = null

    var importedScripts: List<IrScriptSymbol>? = null

    var earlierScripts: List<IrScriptSymbol>? = null

    var targetClass: IrClassSymbol? = null

    var constructor: IrConstructor? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitScript(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
        thisReceiver?.accept(visitor, data)
        explicitCallParameters.forEach { it.accept(visitor, data) }
        implicitReceiversParameters.forEach { it.accept(visitor, data) }
        providedPropertiesParameters.forEach { it.accept(visitor, data) }
        earlierScriptsParameter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        statements.transformInPlace(transformer, data)
        thisReceiver = thisReceiver?.transform(transformer, data)
        explicitCallParameters = explicitCallParameters.transformIfNeeded(transformer, data)
        implicitReceiversParameters = implicitReceiversParameters.transformIfNeeded(transformer, data)
        providedPropertiesParameters = providedPropertiesParameters.transformIfNeeded(transformer, data)
        earlierScriptsParameter = earlierScriptsParameter?.transform(transformer, data)
    }
}
