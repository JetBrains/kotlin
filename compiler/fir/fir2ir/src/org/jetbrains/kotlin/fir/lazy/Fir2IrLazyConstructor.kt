/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.Fir2IrConstructorSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyConstructor(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirConstructor,
    override val symbol: Fir2IrConstructorSymbol,
) : IrConstructor(), AbstractFir2IrLazyDeclaration<FirConstructor>, Fir2IrTypeParametersContainer,
    Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir, symbol)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override lateinit var typeParameters: List<IrTypeParameter>
    override lateinit var parent: IrDeclarationParent

    override var isPrimary: Boolean
        get() = fir.isPrimary
        set(_) = mutationNotSupported()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = symbol.descriptor

    override var isInline: Boolean
        get() = fir.isInline
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = fir.isExpect
        set(_) = mutationNotSupported()

    override var body: IrBody? = null

    override var name: Name
        get() = SpecialNames.INIT
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var returnType: IrType by lazyVar(lock) {
        fir.returnTypeRef.toIrType(typeConverter)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar(lock) {
        val containingClass = parent as? IrClass
        val outerClass = containingClass?.parentClassOrNull
        if (containingClass?.isInner == true && outerClass != null) {
            declarationStorage.enterScope(this.symbol)
            declareThisReceiverParameter(
                thisType = outerClass.thisReceiver!!.type,
                thisOrigin = origin
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazyConstructor.symbol)
            }
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? = null

    override var contextReceiverParametersCount: Int = fir.contextReceivers.size

    override var valueParameters: List<IrValueParameter> by lazyVar(lock) {
        declarationStorage.enterScope(this.symbol)

        buildList {
            declarationStorage.addContextReceiverParametersTo(
                fir.contextReceivers,
                this@Fir2IrLazyConstructor,
                this@buildList,
            )

            fir.valueParameters.mapIndexedTo(this) { index, valueParameter ->
                declarationStorage.createIrParameter(
                    valueParameter, index + contextReceiverParametersCount,
                    useStubForDefaultValueStub = (parent as? IrClass)?.name != Name.identifier("Enum"),
                    forcedDefaultValueConversion = (parent as? IrClass)?.isAnnotationClass == true,
                ).apply {
                    this.parent = this@Fir2IrLazyConstructor
                }
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazyConstructor.symbol)
        }
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstructor(this, data)
    }
}
