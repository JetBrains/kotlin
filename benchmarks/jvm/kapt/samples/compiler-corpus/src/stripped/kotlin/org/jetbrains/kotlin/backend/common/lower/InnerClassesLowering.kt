/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

interface InnerClassesSupport {
    fun getOuterThisField(innerClass: IrClass): IrField
    fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor
    fun getInnerClassOriginalPrimaryConstructorOrNull(innerClass: IrClass): IrConstructor?
}

/**
 * Adds 'outer this' fields to inner classes.
 */
open class InnerClassesLowering(val context: CommonBackendContext) : DeclarationTransformer {
    private val innerClassesSupport = context.innerClassesSupport

    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }

    private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor { TODO() }

    private fun IrConstructor.shouldInitializeOuterThis(): Boolean { TODO() }
}

private fun InnerClassesSupport.primaryConstructorParameterMap(originalConstructor: IrConstructor): Map<IrValueParameter, IrValueParameter> { TODO() }

/**
 * Replaces `this` with 'outer this' field references.
 */
@PhasePrerequisites(InnerClassesLowering::class)
open class InnerClassesMemberBodyLowering(val context: CommonBackendContext) : BodyLoweringPass {
    private val innerClassesSupport = context.innerClassesSupport

    override fun lower(irFile: IrFile) { TODO() }

    private val IrValueSymbol.classForImplicitThis: IrClass?
        // TODO: is this the correct way to get the class?
        get() =
            if (this is IrValueParameterSymbol && owner.kind == IrParameterKind.DispatchReceiver) {
                owner.type.classOrNull?.owner
            } else
                null

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    private fun IrBody.fixThisReference(irClass: IrClass, container: IrDeclaration) { TODO() }
}

open class InnerClassConstructorCallsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    private val innerClassesSupport: InnerClassesSupport = context.innerClassesSupport

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}
