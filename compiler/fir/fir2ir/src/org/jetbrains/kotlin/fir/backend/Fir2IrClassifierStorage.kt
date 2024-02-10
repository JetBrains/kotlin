/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class Fir2IrClassifierStorage(
    private val components: Fir2IrComponents,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by components {
    private val classCache: MutableMap<FirRegularClass, IrClass> = commonMemberStorage.classCache

    private val typeAliasCache: MutableMap<FirTypeAlias, IrTypeAlias> = mutableMapOf()

    private val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = commonMemberStorage.typeParameterCache

    private val typeParameterCacheForSetter: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    private val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntry> = commonMemberStorage.enumEntryCache

    private val codeFragmentCache: MutableMap<FirCodeFragment, IrClass> = mutableMapOf()

    private val fieldsForContextReceivers: MutableMap<IrClass, List<IrField>> = mutableMapOf()

    private val localStorage: Fir2IrLocalClassStorage = Fir2IrLocalClassStorage(
        // Using existing cache is necessary here to be able to serialize local classes from common code in expression codegen
        commonMemberStorage.localClassCache
    )

    private val localClassesCreatedOnTheFly: MutableMap<FirClass, IrClass> = mutableMapOf()

    /**
     * This function is quite messy and doesn't have a good contract of what exactly is traversed.
     * The basic idea is to traverse the symbols which can be reasonably referenced from other modules.
     *
     * Be careful when using it, and avoid it, except really needed.
     */
    @DelicateDeclarationStorageApi
    fun forEachCachedDeclarationSymbol(block: (IrSymbol) -> Unit) {
        classCache.values.forEach { block(it.symbol) }
        typeAliasCache.values.forEach { block(it.symbol) }
        enumEntryCache.values.forEach { block(it.symbol) }
        fieldsForContextReceivers.values.forEach { fields ->
            fields.forEach { block(it.symbol) }
        }
    }

    private var processMembersOfClassesOnTheFlyImmediately = false

    private fun FirTypeRef.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeOrigin) }

    // ------------------------------------ type parameters ------------------------------------

    // Note: declareTypeParameters should be called before!
    internal fun preCacheTypeParameters(owner: FirTypeParameterRefsOwner, irOwnerSymbol: IrSymbol) {
        for ((index, typeParameter) in owner.typeParameters.withIndex()) {
            val original = typeParameter.symbol.fir
            getCachedIrTypeParameter(original)
                ?: createAndCacheIrTypeParameter(original, index, irOwnerSymbol)
            if (owner is FirProperty && owner.isVar) {
                val context = ConversionTypeOrigin.SETTER
                getCachedIrTypeParameter(original, context)
                    ?: createAndCacheIrTypeParameter(original, index, irOwnerSymbol, context)
            }
        }
    }

    internal fun getIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int,
        ownerSymbol: IrSymbol,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ): IrTypeParameter {
        getCachedIrTypeParameter(typeParameter, typeOrigin)?.let { return it }
        val irTypeParameter = createAndCacheIrTypeParameter(typeParameter, index, ownerSymbol, typeOrigin)
        classifiersGenerator.initializeTypeParameterBounds(typeParameter, irTypeParameter)
        return irTypeParameter
    }

    private fun createAndCacheIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int,
        ownerSymbol: IrSymbol,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
    ): IrTypeParameter {
        val symbol = createTypeParameterSymbol(ownerSymbol, index)
        val irTypeParameter = classifiersGenerator.createIrTypeParameterWithoutBounds(typeParameter, index, symbol)
        // Cache the type parameter BEFORE processing its bounds/supertypes, to properly handle recursive type bounds.
        if (typeOrigin.forSetter) {
            typeParameterCacheForSetter[typeParameter] = irTypeParameter
        } else {
            typeParameterCache[typeParameter] = irTypeParameter
        }
        return irTypeParameter
    }

    private fun createTypeParameterSymbol(ownerSymbol: IrSymbol, index: Int): IrTypeParameterSymbol {
        if (ownerSymbol !is IrClassifierSymbol) return IrTypeParameterSymbolImpl()
        val signature = signatureComposer.composeTypeParameterSignature(index, ownerSymbol.signature)
            ?: return IrTypeParameterSymbolImpl()
        return symbolTable.referenceTypeParameter(signature)
    }

    internal fun getCachedIrTypeParameter(
        typeParameter: FirTypeParameter,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ): IrTypeParameter? {
        return if (typeOrigin.forSetter)
            typeParameterCacheForSetter[typeParameter]
        else
            typeParameterCache[typeParameter]
    }

    fun getIrTypeParameterSymbol(
        firTypeParameterSymbol: FirTypeParameterSymbol,
        typeOrigin: ConversionTypeOrigin
    ): IrTypeParameterSymbol {
        val firTypeParameter = firTypeParameterSymbol.fir

        val cachedSymbol = getCachedIrTypeParameter(firTypeParameter, typeOrigin)?.symbol
            ?: typeParameterCache[firTypeParameter]?.symbol // We can try to use default cache because setter can use parent type parameters

        if (cachedSymbol != null) {
            return cachedSymbol
        }

        if (components.configuration.allowNonCachedDeclarations) {
            return createIrTypeParameterForNonCachedDeclaration(firTypeParameter)
        }

        error("Cannot find cached type parameter by FIR symbol: ${firTypeParameterSymbol.name} of the owner: ${firTypeParameter.containingDeclarationSymbol}")
    }

    private fun createIrTypeParameterForNonCachedDeclaration(firTypeParameter: FirTypeParameter): IrTypeParameterSymbol {
        val firTypeParameterOwnerSymbol = firTypeParameter.containingDeclarationSymbol
        val firTypeParameterOwner = firTypeParameterOwnerSymbol.fir as FirTypeParameterRefsOwner
        val index = firTypeParameterOwner.typeParameters.indexOf(firTypeParameter).also { check(it >= 0) }

        val isSetter = firTypeParameterOwner is FirPropertyAccessor && firTypeParameterOwner.isSetter
        val conversionTypeOrigin = if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT

        return createAndCacheIrTypeParameter(firTypeParameter, index, IrTypeParameterSymbolImpl(), conversionTypeOrigin).also {
            classifiersGenerator.initializeTypeParameterBounds(firTypeParameter, it)
        }.symbol
    }

    // ------------------------------------ classes ------------------------------------

    fun createAndCacheIrClass(
        regularClass: FirRegularClass,
        parent: IrDeclarationParent,
        predefinedOrigin: IrDeclarationOrigin? = null
    ): IrClass {
        val symbol = createClassSymbol(signature = null)
        return classifiersGenerator.createIrClass(regularClass, parent, symbol, predefinedOrigin).also {
            @OptIn(LeakedDeclarationCaches::class)
            cacheIrClass(regularClass, it)
        }
    }

    private fun createClassSymbol(signature: IdSignature?): IrClassSymbol {
        return when {
            signature != null -> symbolTable.referenceClass(signature)
            else -> IrClassSymbolImpl()
        }
    }

    @LeakedDeclarationCaches
    internal fun cacheIrClass(regularClass: FirRegularClass, irClass: IrClass) {
        if (regularClass.visibility == Visibilities.Local) {
            localStorage[regularClass] = irClass
        } else {
            classCache[regularClass] = irClass
        }
    }

    fun getCachedIrClass(klass: FirClass): IrClass? {
        return if (klass is FirAnonymousObject || klass is FirRegularClass && klass.visibility == Visibilities.Local) {
            localStorage[klass]
        } else {
            classCache[klass]
        }
    }

    fun findIrClass(lookupTag: ConeClassLikeLookupTag): IrClass? {
        return if (lookupTag.classId.isLocal) {
            getCachedLocalClass(lookupTag)
        } else {
            val firSymbol = lookupTag.toSymbol(session)
            if (firSymbol is FirClassSymbol) {
                getOrCreateIrClass(firSymbol)
            } else {
                null
            }
        }
    }

    private fun getCachedLocalClass(lookupTag: ConeClassLikeLookupTag): IrClass? {
        return localStorage[lookupTag.toSymbol(session)!!.fir as FirClass]
    }

    fun getOrCreateIrClass(firClassSymbol: FirClassSymbol<*>): IrClass {
        val firClass = firClassSymbol.fir
        classifierStorage.getCachedIrClass(firClass)?.let { return it }
        if (firClass is FirAnonymousObject || firClass is FirRegularClass && firClass.visibility == Visibilities.Local) {
            return createAndCacheLocalIrClassOnTheFly(firClass)
        }
        firClass as FirRegularClass
        val classId = firClassSymbol.classId
        val parentId = classId.outerClassId
        val parentClass = parentId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) }
        val irParent = declarationStorage.findIrParent(classId.packageFqName, parentClass?.toLookupTag(), firClassSymbol, firClass.origin)!!

        // firClass may be referenced by some parent's type parameters as a bound. In that case, getIrClassSymbol will be called recursively.
        classifierStorage.getCachedIrClass(firClass)?.let { return it }

        val symbol = createClassSymbol(signature = null)
        val irClass = lazyDeclarationsGenerator.createIrLazyClass(firClass, irParent, symbol)
        classCache[firClass] = irClass
        // NB: this is needed to prevent recursions in case of self bounds
        irClass.prepareTypeParameters()

        return irClass

    }

    fun getFieldsWithContextReceiversForClass(irClass: IrClass, klass: FirClass): List<IrField> {
        if (klass !is FirRegularClass || klass.contextReceivers.isEmpty()) return emptyList()

        return fieldsForContextReceivers.getOrPut(irClass) {
            klass.contextReceivers.withIndex().map { (index, contextReceiver) ->
                components.irFactory.createField(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = IrDeclarationOrigin.FIELD_FOR_CLASS_CONTEXT_RECEIVER,
                    name = Name.identifier("contextReceiverField$index"),
                    visibility = DescriptorVisibilities.PRIVATE,
                    symbol = IrFieldSymbolImpl(),
                    type = contextReceiver.typeRef.toIrType(),
                    isFinal = true,
                    isStatic = false,
                    isExternal = false,
                ).also {
                    it.parent = irClass
                }
            }
        }
    }

    // ------------------------------------ local classes ------------------------------------

    private fun createAndCacheLocalIrClassOnTheFly(klass: FirClass): IrClass {
        val (irClass, firClassOrLocalParent, irClassOrLocalParent) = classifiersGenerator.createLocalIrClassOnTheFly(klass, processMembersOfClassesOnTheFlyImmediately)
        if (!processMembersOfClassesOnTheFlyImmediately) {
            localClassesCreatedOnTheFly[firClassOrLocalParent] = irClassOrLocalParent
        }
        return irClass
    }

    // Note: this function is called exactly once, right after Fir2IrConverter finished f/o binding for regular classes
    fun processMembersOfClassesCreatedOnTheFly() {
        // After the call of this function, members of local classes may be processed immediately
        // Before the call it's not possible, because f/o binding for regular classes isn't done yet
        processMembersOfClassesOnTheFlyImmediately = true
        for ((klass, irClass) in localClassesCreatedOnTheFly) {
            conversionScope.withContainingFirClass(klass) {
                classifiersGenerator.processClassHeader(klass, irClass)
                converter.processClassMembers(klass, irClass)
                // See the problem from KT-57441
                //
                // ```kt
                // class Wrapper {
                //     private val dummy = object: Bar {}
                //     private val bar = object: Bar by dummy {}
                // }
                // interface Bar {
                //     val foo: String
                //         get() = ""
                // }
                // ```
                //
                // When we are building bar.foo fake override, we should call dummy.foo,
                // so we should have object : Bar.foo fake override to be built and bound.
                converter.bindFakeOverridesInClass(irClass)
            }
        }
        localClassesCreatedOnTheFly.clear()
    }


    // ------------------------------------ anonymous objects ------------------------------------

    fun createAndCacheAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.Local,
        name: Name = SpecialNames.NO_NAME_PROVIDED,
        irParent: IrDeclarationParent? = null
    ): IrClass {
        return classifiersGenerator.createAnonymousObject(anonymousObject, visibility, name, irParent).also {
            localStorage[anonymousObject] = it
        }
    }

    fun getIrAnonymousObjectForEnumEntry(anonymousObject: FirAnonymousObject, name: Name, irParent: IrClass?): IrClass {
        localStorage[anonymousObject]?.let { return it }
        val irAnonymousObject = classifierStorage.createAndCacheAnonymousObject(anonymousObject, Visibilities.Private, name, irParent)
        classifiersGenerator.processClassHeader(anonymousObject, irAnonymousObject)
        return irAnonymousObject
    }

    // ------------------------------------ enum entries ------------------------------------

    fun putEnumEntryClassInScope(enumEntry: FirEnumEntry, correspondingClass: IrClass) {
        localStorage[(enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject] = correspondingClass
    }

    internal fun getCachedIrEnumEntry(enumEntry: FirEnumEntry): IrEnumEntry? {
        return enumEntryCache[enumEntry]
    }

    fun getOrCreateIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
    ): IrEnumEntry {
        getCachedIrEnumEntry(enumEntry)?.let { return it }

        val containingFile = firProvider.getFirCallableContainerFile(enumEntry.symbol)

        @Suppress("NAME_SHADOWING")
        val predefinedOrigin = predefinedOrigin ?: if (containingFile != null) {
            IrDeclarationOrigin.DEFINED
        } else {
            irParent.origin
        }
        val symbol = IrEnumEntrySymbolImpl()
        return classifiersGenerator.createIrEnumEntry(
            enumEntry,
            irParent = irParent,
            symbol,
            predefinedOrigin = predefinedOrigin
        ).also {
            enumEntryCache[enumEntry] = it
        }
    }

    // ------------------------------------ typealiases ------------------------------------

    fun createAndCacheIrTypeAlias(
        typeAlias: FirTypeAlias,
        parent: IrDeclarationParent
    ): IrTypeAlias {
        val symbol = IrTypeAliasSymbolImpl()
        return classifiersGenerator.createIrTypeAlias(typeAlias, parent, symbol).also {
            typeAliasCache[typeAlias] = it
        }
    }

    internal fun getCachedTypeAlias(firTypeAlias: FirTypeAlias): IrTypeAlias? = typeAliasCache[firTypeAlias]

    fun referenceTypeAlias(firTypeAliasSymbol: FirTypeAliasSymbol): IrTypeAlias {
        val firTypeAlias = firTypeAliasSymbol.fir
        classifierStorage.getCachedTypeAlias(firTypeAlias)?.let { return it }

        val typeAliasId = firTypeAliasSymbol.classId
        val parentId = typeAliasId.outerClassId
        val parentClass = parentId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) }
        val irParent = declarationStorage.findIrParent(
            typeAliasId.packageFqName,
            parentClass?.toLookupTag(),
            firTypeAliasSymbol,
            firTypeAlias.origin
        )!!

        val symbol = IrTypeAliasSymbolImpl()
        val irTypeAlias = lazyDeclarationsGenerator.createIrLazyTypeAlias(firTypeAlias, irParent, symbol)
        typeAliasCache[firTypeAlias] = irTypeAlias
        irTypeAlias.prepareTypeParameters()

        return irTypeAlias
    }

    // ------------------------------------ code fragments ------------------------------------

    fun getCachedIrCodeFragment(codeFragment: FirCodeFragment): IrClass? {
        return codeFragmentCache[codeFragment]
    }

    fun createAndCacheCodeFragmentClass(codeFragment: FirCodeFragment, containingFile: IrFile): IrClass {
        val signature = signatureComposer.composeSignature(codeFragment)
        val symbol = createClassSymbol(signature)
        return classifiersGenerator.createCodeFragmentClass(codeFragment, containingFile, symbol).also {
            codeFragmentCache[codeFragment] = it
        }
    }
}
