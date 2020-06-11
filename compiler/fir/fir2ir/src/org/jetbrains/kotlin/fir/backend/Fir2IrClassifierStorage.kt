/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrEnumEntryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeAliasImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name

class Fir2IrClassifierStorage(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {
    private val firProvider = session.firProvider

    private val classCache = mutableMapOf<FirRegularClass, IrClass>()

    private val typeParameterCache = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val typeParameterCacheForSetter = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val enumEntryCache = mutableMapOf<FirEnumEntry, IrEnumEntry>()

    private val localStorage = Fir2IrLocalStorage()

    private fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    fun preCacheBuiltinClasses() {
        for ((classId, irBuiltinSymbol) in typeConverter.classIdToSymbolMap) {
            val firClass = ConeClassLikeLookupTagImpl(classId).toSymbol(session)!!.fir as FirRegularClass
            val irClass = irBuiltinSymbol.owner
            classCache[firClass] = irClass
            processClassHeader(firClass, irClass)
            declarationStorage.preCacheBuiltinClassMembers(firClass, irClass)
        }
        for ((primitiveClassId, primitiveArrayId) in StandardClassIds.primitiveArrayTypeByElementType) {
            val firClass = ConeClassLikeLookupTagImpl(primitiveArrayId).toSymbol(session)!!.fir as FirRegularClass
            val irType = typeConverter.classIdToTypeMap[primitiveClassId]
            val irClass = irBuiltIns.primitiveArrayForType[irType]!!.owner
            classCache[firClass] = irClass
            processClassHeader(firClass, irClass)
            declarationStorage.preCacheBuiltinClassMembers(firClass, irClass)
        }
    }

    private fun IrClass.setThisReceiver(typeParameters: List<FirTypeParameterRef>) {
        symbolTable.enterScope(this)
        val typeArguments = typeParameters.map {
            IrSimpleTypeImpl(getCachedIrTypeParameter(it.symbol.fir)!!.symbol, false, emptyList(), emptyList())
        }
        thisReceiver = declareThisReceiverParameter(
            symbolTable,
            thisType = IrSimpleTypeImpl(symbol, false, typeArguments, emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
    }

    internal fun preCacheTypeParameters(owner: FirTypeParameterRefsOwner) {
        for ((index, typeParameter) in owner.typeParameters.withIndex()) {
            val original = typeParameter.symbol.fir
            getCachedIrTypeParameter(original, index)
                ?: createIrTypeParameterWithoutBounds(original, index)
            if (owner is FirProperty && owner.isVar) {
                val context = ConversionTypeContext.DEFAULT.inSetter()
                getCachedIrTypeParameter(original, index, context)
                    ?: createIrTypeParameterWithoutBounds(original, index, context)
            }
        }
    }

    internal fun IrTypeParametersContainer.setTypeParameters(
        owner: FirTypeParameterRefsOwner,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ) {
        typeParameters = owner.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            getIrTypeParameter(typeParameter, index, typeContext).apply {
                parent = this@setTypeParameters
                if (superTypes.isEmpty()) {
                    typeParameter.bounds.mapTo(superTypes) { it.toIrType() }
                }
            }
        }
    }

    private fun IrClass.declareTypeParameters(klass: FirClass<*>) {
        if (klass is FirRegularClass) {
            preCacheTypeParameters(klass)
            setTypeParameters(klass)
        }
    }

    private fun IrClass.declareSupertypes(klass: FirClass<*>) {
        superTypes = klass.superTypeRefs.map { superTypeRef -> superTypeRef.toIrType() }
    }

    private fun IrClass.declareSupertypesAndTypeParameters(klass: FirClass<*>): IrClass {
        declareTypeParameters(klass)
        declareSupertypes(klass)
        return this
    }

    fun getCachedIrClass(klass: FirClass<*>): IrClass? {
        return if (klass is FirAnonymousObject || klass is FirRegularClass && klass.visibility == Visibilities.LOCAL) {
            localStorage.getLocalClass(klass)
        } else {
            classCache[klass]
        }
    }

    private fun FirRegularClass.enumClassModality(): Modality {
        return when {
            declarations.any { it is FirCallableMemberDeclaration<*> && it.modality == Modality.ABSTRACT } -> {
                Modality.ABSTRACT
            }
            declarations.any { it is FirEnumEntry && it.initializer != null } -> {
                Modality.OPEN
            }
            else -> {
                Modality.FINAL
            }
        }
    }

    internal fun createIrClass(klass: FirClass<*>, parent: IrDeclarationParent? = null): IrClass {
        // NB: klass can be either FirRegularClass or FirAnonymousObject
        if (klass is FirAnonymousObject) {
            return createIrAnonymousObject(klass, irParent = parent)
        }
        val regularClass = klass as FirRegularClass
        val origin = regularClass.irOrigin(firProvider)
        val irClass = registerIrClass(regularClass, parent, origin)
        processClassHeader(regularClass, irClass)
        return irClass
    }

    fun processClassHeader(regularClass: FirRegularClass, irClass: IrClass = getCachedIrClass(regularClass)!!): IrClass {
        irClass.declareTypeParameters(regularClass)
        irClass.setThisReceiver(regularClass.typeParameters)
        irClass.declareSupertypes(regularClass)
        return irClass
    }

    fun registerTypeAlias(
        typeAlias: FirTypeAlias,
        parent: IrFile
    ): IrTypeAlias {
        val signature = signatureComposer.composeSignature(typeAlias)!!
        preCacheTypeParameters(typeAlias)
        return typeAlias.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareTypeAlias(signature, { Fir2IrTypeAliasSymbol(signature) }) { symbol ->
                IrTypeAliasImpl(
                    startOffset, endOffset, symbol,
                    typeAlias.name, typeAlias.visibility,
                    typeAlias.expandedTypeRef.toIrType(),
                    typeAlias.isActual, IrDeclarationOrigin.DEFINED
                ).apply {
                    this.parent = parent
                    setTypeParameters(typeAlias)
                    parent.declarations += this
                }
            }
        }
    }

    private fun declareIrClass(signature: IdSignature?, factory: (IrClassSymbol) -> IrClass): IrClass {
        if (signature == null) {
            val descriptor = WrappedClassDescriptor()
            return symbolTable.declareClass(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareClass(signature, { Fir2IrClassSymbol(signature) }, factory)
    }

    fun registerIrClass(
        regularClass: FirRegularClass,
        parent: IrDeclarationParent? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrClass {
        val visibility = regularClass.visibility
        val modality = if (regularClass.classKind == ClassKind.ENUM_CLASS) {
            regularClass.enumClassModality()
        } else {
            regularClass.modality ?: Modality.FINAL
        }
        val signature = signatureComposer.composeSignature(regularClass)
        val irClass = regularClass.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { symbol ->
                IrClassImpl(
                    startOffset,
                    endOffset,
                    origin,
                    symbol,
                    regularClass.name,
                    regularClass.classKind,
                    visibility,
                    modality,
                    isCompanion = regularClass.isCompanion,
                    isInner = regularClass.isInner,
                    isData = regularClass.isData,
                    isExternal = regularClass.isExternal,
                    isInline = regularClass.isInline,
                    isExpect = regularClass.isExpect,
                    isFun = regularClass.isFun
                ).apply {
                    metadata = FirMetadataSource.Class(regularClass)
                }
            }
        }
        if (parent != null) {
            irClass.parent = parent
        }
        if (regularClass.visibility == Visibilities.LOCAL) {
            localStorage.putLocalClass(regularClass, irClass)
        } else {
            classCache[regularClass] = irClass
        }
        return irClass
    }

    fun createIrAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.LOCAL,
        name: Name = Name.special("<no name provided>"),
        irParent: IrDeclarationParent? = null
    ): IrClass {
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        val signature = signatureComposer.composeSignature(anonymousObject)
        val result = anonymousObject.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { symbol ->
                IrClassImpl(
                    startOffset, endOffset, origin, symbol, name,
                    // NB: for unknown reason, IR uses 'CLASS' kind for simple anonymous objects
                    anonymousObject.classKind.takeIf { it == ClassKind.ENUM_ENTRY } ?: ClassKind.CLASS,
                    visibility, modality
                ).apply {
                    metadata = FirMetadataSource.Class(anonymousObject)
                    setThisReceiver(anonymousObject.typeParameters)
                    if (irParent != null) {
                        this.parent = irParent
                    }
                }
            }
        }.declareSupertypesAndTypeParameters(anonymousObject)
        localStorage.putLocalClass(anonymousObject, result)
        return result
    }

    private fun getIrAnonymousObjectForEnumEntry(anonymousObject: FirAnonymousObject, name: Name, irParent: IrClass?): IrClass {
        localStorage.getLocalClass(anonymousObject)?.let { return it }
        return createIrAnonymousObject(anonymousObject, Visibilities.PRIVATE, name, irParent)
    }

    private fun createIrTypeParameterWithoutBounds(
        typeParameter: FirTypeParameter,
        index: Int,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter {
        require(index >= 0)
        val descriptor = WrappedTypeParameterDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val irTypeParameter = with(typeParameter) {
            convertWithOffsets { startOffset, endOffset ->
                symbolTable.declareGlobalTypeParameter(startOffset, endOffset, origin, descriptor) { symbol ->
                    IrTypeParameterImpl(
                        startOffset, endOffset, origin, symbol,
                        name, if (index < 0) 0 else index,
                        isReified,
                        variance
                    ).apply {
                        descriptor.bind(this)
                    }
                }
            }
        }

        // Cache the type parameter BEFORE processing its bounds/supertypes, to properly handle recursive type bounds.
        if (typeContext.origin == ConversionTypeOrigin.SETTER) {
            typeParameterCacheForSetter[typeParameter] = irTypeParameter
        } else {
            typeParameterCache[typeParameter] = irTypeParameter
        }
        return irTypeParameter
    }

    internal fun getCachedIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int = -1,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter? {
        // Here transformation is a bit difficult because one FIR property type parameter
        // can be transformed to two different type parameters: one for getter and another one for setter
        val simpleCachedParameter = typeParameterCache[typeParameter]
        if (simpleCachedParameter != null) {
            if (typeContext.origin != ConversionTypeOrigin.SETTER) {
                return simpleCachedParameter
            }
            if (index < 0) {
                val parent = simpleCachedParameter.parent
                if (parent !is IrSimpleFunction || parent.returnType == irBuiltIns.unitType) {
                    return simpleCachedParameter
                }
            }
        }
        if (typeContext.origin == ConversionTypeOrigin.SETTER) {
            typeParameterCacheForSetter[typeParameter]?.let { return it }
        }
        return null
    }

    internal fun getIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter {
        getCachedIrTypeParameter(typeParameter, index, typeContext)?.let { return it }
        return typeParameter.run {
            val irTypeParameter = createIrTypeParameterWithoutBounds(typeParameter, index, typeContext)
            bounds.mapTo(irTypeParameter.superTypes) { it.toIrType() }
            irTypeParameter
        }
    }

    fun putEnumEntryClassInScope(enumEntry: FirEnumEntry, correspondingClass: IrClass) {
        localStorage.putLocalClass(enumEntry.initializer as FirAnonymousObject, correspondingClass)
    }

    fun getCachedIrEnumEntry(enumEntry: FirEnumEntry): IrEnumEntry? = enumEntryCache[enumEntry]

    private fun declareIrEnumEntry(signature: IdSignature?, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        if (signature == null) {
            val descriptor = WrappedEnumEntryDescriptor()
            return symbolTable.declareEnumEntry(0, 0, IrDeclarationOrigin.DEFINED, descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareEnumEntry(signature, { Fir2IrEnumEntrySymbol(signature) }, factory)
    }

    fun createIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass?,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrEnumEntry {
        return enumEntry.convertWithOffsets { startOffset, endOffset ->
            val signature = signatureComposer.composeSignature(enumEntry)
            val result = declareIrEnumEntry(signature) { symbol ->
                IrEnumEntryImpl(
                    startOffset, endOffset, origin, symbol, enumEntry.name
                ).apply {
                    declarationStorage.enterScope(this)
                    val irType = enumEntry.returnTypeRef.toIrType()
                    if (irParent != null) {
                        this.parent = irParent
                    }
                    val initializer = enumEntry.initializer
                    if (initializer != null) {
                        initializer as FirAnonymousObject
                        val klass = getIrAnonymousObjectForEnumEntry(initializer, enumEntry.name, irParent)

                        this.correspondingClass = klass
                    } else if (irParent != null && origin == IrDeclarationOrigin.DEFINED) {
                        val constructor = irParent.constructors.first()
                        this.initializerExpression = IrExpressionBodyImpl(
                            IrEnumConstructorCallImpl(
                                startOffset, endOffset, irType, constructor.symbol,
                                valueArgumentsCount = constructor.valueParameters.size,
                                typeArgumentsCount = constructor.typeParameters.size
                            )
                        )
                    }
                    declarationStorage.leaveScope(this)
                }
            }
            enumEntryCache[enumEntry] = result
            result
        }
    }

    fun getIrClassSymbol(firClassSymbol: FirClassSymbol<*>): IrClassSymbol {
        val firClass = firClassSymbol.fir
        getCachedIrClass(firClass)?.let { return it.symbol }
        if (firClass is FirAnonymousObject || firClass is FirRegularClass && firClass.visibility == Visibilities.LOCAL) {
            return createIrClass(firClass).symbol
        }
        val signature = signatureComposer.composeSignature(firClass)!!
        symbolTable.referenceClassIfAny(signature)?.let { irClassSymbol ->
            val irClass = irClassSymbol.owner
            classCache[firClass as FirRegularClass] = irClass
            processClassHeader(firClass, irClass)
            declarationStorage.preCacheBuiltinClassMembers(firClass, irClass)
            return irClassSymbol
        }
        firClass as FirRegularClass
        val classId = firClassSymbol.classId
        val parentId = classId.outerClassId
        val irParent = declarationStorage.findIrParent(classId.packageFqName, parentId, firClassSymbol)!!
        val symbol = Fir2IrClassSymbol(signature)
        val irClass = firClass.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareClass(signature, { symbol }) {
                Fir2IrLazyClass(components, startOffset, endOffset, firClass.irOrigin(firProvider), firClass, symbol).apply {
                    parent = irParent
                }
            }
        }
        classCache[firClass] = irClass
        // NB: this is needed to prevent recursions in case of self bounds
        (irClass as Fir2IrLazyClass).prepareTypeParameters()

        return symbol
    }

    fun getIrTypeParameterSymbol(
        firTypeParameterSymbol: FirTypeParameterSymbol,
        typeContext: ConversionTypeContext
    ): IrTypeParameterSymbol {
        return getCachedIrTypeParameter(firTypeParameterSymbol.fir, typeContext = typeContext)?.symbol
            ?: throw AssertionError("Cannot find cached type parameter by FIR symbol: ${firTypeParameterSymbol.name}")
    }
}