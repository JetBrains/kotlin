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
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyClass(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    fir: FirRegularClass,
    symbol: Fir2IrClassSymbol
) : AbstractFir2IrLazyDeclaration<FirRegularClass, IrClass>(
    components, startOffset, endOffset, origin, fir, symbol
), IrClass {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = super.descriptor as ClassDescriptor

    override val symbol: Fir2IrClassSymbol
        get() = super.symbol as Fir2IrClassSymbol

    override val name: Name
        get() = fir.name

    override var visibility: Visibility
        get() = fir.visibility
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
        val scope = fir.buildUseSiteMemberScope(session, scopeSession)!!
        scope.processDeclaredConstructors {
            result += declarationStorage.getIrConstructorSymbol(it).owner
        }
        for (declaration in fir.declarations) {
            when (declaration) {
                is FirSimpleFunction -> {
                    if (declaration.name !in processedNames) {
                        processedNames += declaration.name
                        scope.processFunctionsByName(declaration.name) {
                            if (it is FirNamedFunctionSymbol && it.callableId.classId == fir.symbol.classId) {
                                if (it.isAbstractMethodOfAny()) {
                                    return@processFunctionsByName
                                }
                                result += declarationStorage.getIrFunctionSymbol(it).owner
                            }
                        }
                    }
                }
                is FirProperty -> {
                    if (declaration.name !in processedNames) {
                        processedNames += declaration.name
                        scope.processPropertiesByName(declaration.name) {
                            if (it is FirPropertySymbol) {
                                result += declarationStorage.getIrPropertyOrFieldSymbol(it).owner as IrProperty
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
            result += getFakeOverrides(fir, processedNames)
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

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        thisReceiver?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        thisReceiver = thisReceiver?.transform(transformer, data)
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
        declarations.transform { it.transform(transformer, data) }
    }
}
