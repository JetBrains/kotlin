/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isNonCompanionObject
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

class Fir2IrLazySimpleFunction(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirSimpleFunction,
    firParent: FirRegularClass,
    override val symbol: Fir2IrSimpleFunctionSymbol,
    override val isFakeOverride: Boolean
) : IrSimpleFunction(), AbstractFir2IrLazyDeclaration<FirSimpleFunction, IrSimpleFunction>, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
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

    override var body: IrBody? = null

    override val name: Name
        get() = fir.name

    @Suppress("SetterBackingFieldAssignment")
    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val modality: Modality
        get() = fir.modality!!

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var returnType: IrType by lazyVar {
        fir.returnTypeRef.toIrType(typeConverter)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        val containingClass = parent as? IrClass
        if (containingClass != null && !fir.isStatic &&
            !(containingClass.isNonCompanionObject && hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME))
        ) {
            declarationStorage.enterScope(this)
            declareThisReceiverParameter(
                symbolTable,
                thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"),
                thisOrigin = origin
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
            }
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar {
        fir.receiverTypeRef?.let {
            declarationStorage.enterScope(this)
            declareThisReceiverParameter(
                symbolTable,
                thisType = it.toIrType(typeConverter),
                thisOrigin = origin,
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
            }
        }
    }

    override var valueParameters: List<IrValueParameter> by lazyVar {
        declarationStorage.enterScope(this)
        fir.valueParameters.mapIndexed { index, valueParameter ->
            declarationStorage.createIrParameter(
                valueParameter, index,
            ).apply {
                this.parent = this@Fir2IrLazySimpleFunction
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
        }
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by lazyVar {
        fir.generateOverriddenFunctionSymbols(firParent, session, scopeSession, declarationStorage)
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource
}
