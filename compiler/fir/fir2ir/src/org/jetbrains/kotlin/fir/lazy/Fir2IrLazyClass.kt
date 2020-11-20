/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyClass(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirRegularClass,
    override val symbol: Fir2IrClassSymbol,
) : IrClass(), AbstractFir2IrLazyDeclaration<FirRegularClass, IrClass>, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override lateinit var typeParameters: List<IrTypeParameter>
    override lateinit var parent: IrDeclarationParent

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override val name: Name
        get() = fir.name

    @Suppress("SetterBackingFieldAssignment")
    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override var modality: Modality
        get() = fir.modality!!
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override var attributeOwnerId: IrAttributeContainer
        get() = this
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val kind: ClassKind
        get() = fir.classKind

    override val isCompanion: Boolean
        get() = fir.isCompanion

    override val isInner: Boolean
        get() = fir.isInner

    override val isData: Boolean
        get() = fir.isData

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isInline: Boolean
        get() = fir.isInline

    override val isExpect: Boolean
        get() = fir.isExpect

    override val isFun: Boolean
        get() = fir.isFun

    override var superTypes: List<IrType> by lazyVar {
        fir.superTypeRefs.map { it.toIrType(typeConverter) }
    }

    override var thisReceiver: IrValueParameter? by lazyVar {
        symbolTable.enterScope(this)
        val typeArguments = fir.typeParameters.map {
            IrSimpleTypeImpl(
                classifierStorage.getCachedIrTypeParameter(it.symbol.fir)!!.symbol,
                hasQuestionMark = false, arguments = emptyList(), annotations = emptyList()
            )
        }
        val receiver = declareThisReceiverParameter(
            symbolTable,
            thisType = IrSimpleTypeImpl(symbol, hasQuestionMark = false, arguments = typeArguments, annotations = emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
        receiver
    }

    override val declarations: MutableList<IrDeclaration> by lazyVar {
        val result = mutableListOf<IrDeclaration>()
        val processedNames = mutableSetOf<Name>()
        // NB: it's necessary to take all callables from scope,
        // e.g. to avoid accessing un-enhanced Java declarations with FirJavaTypeRef etc. inside
        val scope = fir.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        scope.processDeclaredConstructors {
            result += declarationStorage.getIrConstructorSymbol(it).owner
        }
        for (declaration in fir.declarations) {
            when (declaration) {
                is FirSimpleFunction -> {
                    if (declaration.name !in processedNames) {
                        processedNames += declaration.name
                        if (fir.classKind == ClassKind.ENUM_CLASS && declaration.isStatic &&
                            declaration.returnTypeRef is FirResolvedTypeRef
                        ) {
                            // Handle values() / valueOf() separately
                            // TODO: handle other static functions / properties properly
                            result += declarationStorage.getIrFunctionSymbol(declaration.symbol).owner
                        } else {
                            scope.processFunctionsByName(declaration.name) {
                                if (it is FirNamedFunctionSymbol && it.dispatchReceiverClassOrNull() == fir.symbol.toLookupTag()) {
                                    if (it.isAbstractMethodOfAny()) {
                                        return@processFunctionsByName
                                    }
                                    result += declarationStorage.getIrFunctionSymbol(it).owner
                                }
                            }
                        }
                    }
                }
                is FirProperty -> {
                    if (declaration.name !in processedNames) {
                        processedNames += declaration.name
                        scope.processPropertiesByName(declaration.name) {
                            if (it is FirPropertySymbol) {
                                result += declarationStorage.getIrPropertySymbol(it).owner as IrProperty
                            }
                        }
                    }
                }
                is FirRegularClass -> {
                    val nestedSymbol = classifierStorage.getIrClassSymbol(declaration.symbol)
                    result += nestedSymbol.owner
                }
                else -> continue
            }
        }
        with(fakeOverrideGenerator) {
            val fakeOverrides = getFakeOverrides(fir, fir.declarations)
            bindOverriddenSymbols(fakeOverrides)
            result += fakeOverrides
        }
        // TODO: remove this check to save time
        for (declaration in result) {
            if (declaration.parent != this) {
                throw AssertionError("Unmatched parent for lazy class member")
            }
        }
        result
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    private fun FirNamedFunctionSymbol.isAbstractMethodOfAny(): Boolean {
        val fir = fir
        if (fir.modality != Modality.ABSTRACT) return false
        return when (fir.name.asString()) {
            "equals" -> fir.valueParameters.singleOrNull()?.returnTypeRef?.isNullableAny == true
            "hashCode", "toString" -> fir.valueParameters.isEmpty()
            else -> false
        }
    }
}
