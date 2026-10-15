/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Merges init block and field initializers into constructors.
 */
open class InitializersLowering(context: CommonBackendContext) : InitializersLoweringBase(context), BodyLoweringPass {
    override fun lower(irFile: IrFile) { TODO() }

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}

private val IrField.primaryConstructorParameter: Boolean
    get() = (initializer?.expression as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER

abstract class InitializersLoweringBase(open val context: CommonBackendContext) {
    protected fun extractInitializers(irClass: IrClass, filter: (IrDeclaration) -> Boolean) =
        // TODO What about fields that were added by lowerings? e.g. captured outer class or locals?
        irClass.declarations.mapNotNull { if (it is IrProperty) it.backingField else it }.filter(filter).mapNotNull {
            when (it) {
                is IrField -> handleField(irClass, it)
                is IrAnonymousInitializer -> handleAnonymousInitializer(it)
                else -> null
            }
        }

    private fun handleField(irClass: IrClass, declaration: IrField): IrStatement? = TODO()

    private fun handleAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement = TODO()
}

/**
 * Removes anonymous initializers and sets field initializers to `null`.
 */
open class InitializersCleanupLowering(
    val context: CommonBackendContext,
    private val shouldEraseFieldInitializer: (IrField) -> Boolean = { it.correspondingPropertySymbol?.owner?.isConst != true }
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }
}
