/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.mapOptimized
import org.jetbrains.kotlin.ir.util.transform
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

    internal fun prepareTypeParameters() {
        typeParameters = fir.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index).apply {
                parent = this@Fir2IrLazyClass
                if (superTypes.isEmpty()) {
                    typeParameter.bounds.mapTo(superTypes) { it.toIrType(typeConverter) }
                }
            }
        }
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
            throw AssertionError("Mutating Fir2Ir lazy elements is not possible")
        }

    override var modality: Modality
        get() = fir.modality!!
        set(_) {
            throw AssertionError("Mutating Fir2Ir lazy elements is not possible")
        }

    override var attributeOwnerId: IrAttributeContainer
        get() = this
        set(_) {
            throw AssertionError("Mutating Fir2Ir lazy elements is not possible")
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

    override lateinit var typeParameters: List<IrTypeParameter>

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
            result += declarationStorage.createIrConstructor(it.fir, this)
        }
        for (declaration in fir.declarations) {
            when (declaration) {
                is FirSimpleFunction -> {
                    if (declaration.name !in processedNames) {
                        processedNames += declaration.name
                        scope.processFunctionsByName(declaration.name) {
                            if (it is FirNamedFunctionSymbol) {
                                if (it.isAbstractMethodOfAny()) {
                                    return@processFunctionsByName
                                }
                                result += if (!it.isFakeOverride) {
                                    declarationStorage.createIrFunction(it.fir, irParent = this, origin = origin)
                                } else {
                                    val fakeOverrideSymbol =
                                        FirClassSubstitutionScope.createFakeOverrideFunction(session, it.fir, it)
                                    classifierStorage.preCacheTypeParameters(it.fir)
                                    declarationStorage.createIrFunction(fakeOverrideSymbol.fir, irParent = this)
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
                                result += if (!it.isFakeOverride) {
                                    declarationStorage.createIrProperty(it.fir, irParent = this, origin = origin)
                                } else {
                                    val fakeOverrideSymbol =
                                        FirClassSubstitutionScope.createFakeOverrideProperty(session, it.fir, it)
                                    classifierStorage.preCacheTypeParameters(it.fir)
                                    declarationStorage.createIrProperty(fakeOverrideSymbol.fir, irParent = this)
                                }
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
        for (irDeclaration in result) {
            irDeclaration.parent = this
        }
        result
    }

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
        typeParameters = typeParameters.mapOptimized { it.transform(transformer, data) }
        declarations.transform { it.transform(transformer, data) }
    }
}