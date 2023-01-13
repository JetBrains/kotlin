/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeserializableClass
import org.jetbrains.kotlin.ir.util.isFacadeClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import kotlin.properties.ReadWriteProperty

abstract class AbstractFir2IrLazyFunction<F : FirCallableDeclaration>(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    override val isFakeOverride: Boolean
) : IrSimpleFunction(), AbstractFir2IrLazyDeclaration<F>, Fir2IrTypeParametersContainer, IrLazyFunctionBase,
    Fir2IrComponents by components {

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

    override var body: IrBody? by lazyVar(lock) {
        if (tryLoadIr()) body else null
    }

    override var visibility: DescriptorVisibility by lazyVar(lock) {
        components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
    }

    override val modality: Modality
        get() = fir.modality!!

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    protected fun shouldHaveDispatchReceiver(containingClass: IrClass): Boolean {
        return !fir.isStatic && !containingClass.isFacadeClass &&
                (!containingClass.isObject || containingClass.isCompanion || !hasJvmStaticAnnotation())
    }

    private fun hasJvmStaticAnnotation(): Boolean {
        return fir.hasAnnotation(JVM_STATIC_CLASS_ID, session) ||
                (fir as? FirPropertyAccessor)?.propertySymbol?.fir?.hasAnnotation(JVM_STATIC_CLASS_ID, session) == true
    }

    protected fun createThisReceiverParameter(thisType: IrType, explicitReceiver: FirReceiverParameter? = null): IrValueParameter {
        declarationStorage.enterScope(this)
        return declareThisReceiverParameter(thisType, origin, explicitReceiver = explicitReceiver).apply {
            declarationStorage.leaveScope(this@AbstractFir2IrLazyFunction)
        }
    }

    override val factory: IrFactory
        get() = super<AbstractFir2IrLazyDeclaration>.factory

    override fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> {
        return super<AbstractFir2IrLazyDeclaration>.createLazyAnnotations()
    }

    private fun tryLoadIr(): Boolean {
        if (!isInline || isFakeOverride) return false
        if (!extensions.irNeedsDeserialization) return false
        val toplevel = getToplevel()
        return (toplevel as? DeserializableClass)?.loadIr() ?: false
    }

    private fun getToplevel(): IrDeclaration {
        var current: IrDeclaration = this
        while (current.parent !is IrPackageFragment) {
            current = current.parent as IrDeclaration
        }
        return current
    }

    companion object {
        private val JVM_STATIC_CLASS_ID = ClassId.topLevel(JVM_STATIC_ANNOTATION_FQ_NAME)
    }
}
