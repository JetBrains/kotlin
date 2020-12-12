/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

abstract class AbstractFir2IrLazyFunction<F : FirMemberDeclaration>(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: Fir2IrSimpleFunctionSymbol,
    override val isFakeOverride: Boolean
) : IrSimpleFunction(), AbstractFir2IrLazyDeclaration<F, IrSimpleFunction>, Fir2IrComponents by components {

    override lateinit var typeParameters: List<IrTypeParameter>
    override lateinit var parent: IrDeclarationParent

    override val isTailrec: Boolean
        get() = fir.isTailRec

    override val isSuspend: Boolean
        get() = fir.isSuspend

    override val isOperator: Boolean
        get() = fir.isOperator

    override val isInfix: Boolean
        get() = fir.isInfix

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    override val isInline: Boolean
        get() = fir.isInline

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isExpect: Boolean
        get() = fir.isExpect

    override var body: IrBody?
        get() = null
        set(_) = error("We should never need to store body of external functions.")

    override var visibility: DescriptorVisibility by lazyVar {
        components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
    }

    override val modality: Modality
        get() = fir.modality!!

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    protected fun shouldHaveDispatchReceiver(
        containingClass: IrClass,
        staticOwner: FirCallableMemberDeclaration<*>
    ): Boolean {
        return !staticOwner.isStatic &&
                (!containingClass.isObject || containingClass.isCompanion || !staticOwner.hasAnnotation(JVM_STATIC_CLASS_ID))
    }

    protected fun createThisReceiverParameter(thisType: IrType): IrValueParameter {
        declarationStorage.enterScope(this)
        return declareThisReceiverParameter(symbolTable, thisType, origin).apply {
            declarationStorage.leaveScope(this@AbstractFir2IrLazyFunction)
        }
    }

    companion object {
        private val JVM_STATIC_CLASS_ID = ClassId.topLevel(JVM_STATIC_ANNOTATION_FQ_NAME)
    }
}