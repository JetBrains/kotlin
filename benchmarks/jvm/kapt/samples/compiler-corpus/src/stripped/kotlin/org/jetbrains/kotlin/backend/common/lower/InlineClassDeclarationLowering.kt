/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

private const val INLINE_CLASS_IMPL_SUFFIX = "-impl"

open class InlineClassDeclarationLowering(private val context: CommonBackendContext) : DeclarationTransformer {
    protected open fun processDelegatedInlineClassMember(declaration: IrDeclaration) { TODO() }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }

    private fun transformConstructor(irConstructor: IrConstructor): List<IrDeclaration> { TODO() }


    private fun transformMethodFlat(function: IrSimpleFunction): List<IrDeclaration>? { TODO() }

    private fun transformPrimaryConstructor(irConstructor: IrConstructor): List<IrDeclaration> { TODO() }

    private fun transformConstructorBody(irConstructor: IrConstructor, staticMethod: IrSimpleFunction) { TODO() }

    private fun transformMethodBodyFlat(function: IrSimpleFunction, staticMethod: IrSimpleFunction) { TODO() }

    private fun delegateToStaticMethod(function: IrSimpleFunction, staticMethod: IrSimpleFunction): IrBlockBody { TODO() }

}

open class InlineClassUsageLowering(private val context: CommonBackendContext) : BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}

private fun CommonBackendContext.getOrCreateStaticMethod(function: IrFunction): IrSimpleFunction = TODO()

private fun IrFunction.toInlineClassImplementationName(): Name { TODO() }

private fun CommonBackendContext.createStaticBodilessMethod(function: IrFunction): IrSimpleFunction = TODO()

private var IrFunction.staticMethod: IrSimpleFunction? by irAttribute(copyByDefault = false)
