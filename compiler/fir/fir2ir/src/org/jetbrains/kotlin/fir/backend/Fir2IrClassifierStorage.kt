/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class Fir2IrClassifierStorage(
    private val components: Fir2IrComponents,
    private val dependentStorages: List<Fir2IrClassifierStorage>
) : Fir2IrComponents by components {
    private val firProvider = session.firProvider

    private val classCache: MutableMap<FirRegularClass, IrClass> = merge { it.classCache }

    private val localClassesCreatedOnTheFly: MutableMap<FirClass, IrClass> = mutableMapOf()

    private var processMembersOfClassesOnTheFlyImmediately = false

    private val typeAliasCache: MutableMap<FirTypeAlias, IrTypeAlias> = mutableMapOf()

    private val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = merge { it.typeParameterCache }

    private val typeParameterCacheForSetter: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    private val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntry> = merge { it.enumEntryCache }

    private val fieldsForContextReceivers: MutableMap<IrClass, List<IrField>> = mutableMapOf()

    private val localStorage: Fir2IrLocalClassStorage = Fir2IrLocalClassStorage(
        // Merge is necessary here to be able to serialize local classes from common code in expression codegen
        dependentStorages.map { it.localStorage }.fold(mutableMapOf()) { result, storage ->
            result.putAll(storage.localClassCache)
            result
        }
    )

    private fun <K, V> merge(mapFunc: (Fir2IrClassifierStorage) -> MutableMap<K, V>): MutableMap<K, V> {
        return dependentStorages.map { mapFunc(it) }.fold(mutableMapOf()) { result, map ->
            result.putAll(map)
            result
        }
    }

    private fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    fun preCacheBuiltinClasses() {
        // dependentStorages are only actual for MPP scenario
        // There is no need to precache them twice: the same library session is used and FIR and IR elements are the same
        if (dependentStorages.isNotEmpty()) return
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
            IrSimpleTypeImpl(getIrTypeParameterSymbol(it.symbol, ConversionTypeContext.DEFAULT), false, emptyList(), emptyList())
        }
        thisReceiver = declareThisReceiverParameter(
            thisType = IrSimpleTypeImpl(symbol, false, typeArguments, emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
    }

    internal fun preCacheTypeParameters(owner: FirTypeParameterRefsOwner, irOwnerSymbol: IrSymbol) {
        for ((index, typeParameter) in owner.typeParameters.withIndex()) {
            val original = typeParameter.symbol.fir
            getCachedIrTypeParameter(original)
                ?: createIrTypeParameterWithoutBounds(original, index, irOwnerSymbol)
            if (owner is FirProperty && owner.isVar) {
                val context = ConversionTypeContext.DEFAULT.inSetter()
                getCachedIrTypeParameter(original, context)
                    ?: createIrTypeParameterWithoutBounds(original, index, irOwnerSymbol, context)
            }
        }
    }

    internal fun IrTypeParametersContainer.setTypeParameters(
        owner: FirTypeParameterRefsOwner,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ) {
        typeParameters = owner.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            getIrTypeParameter(typeParameter, index, symbol, typeContext).apply {
                parent = this@setTypeParameters
                if (superTypes.isEmpty()) {
                    superTypes = typeParameter.bounds.map { it.toIrType(typeContext) }
                }
            }
        }
    }

    private fun IrClass.declareTypeParameters(klass: FirClass) {
        if (klass is FirRegularClass) {
            preCacheTypeParameters(klass, symbol)
            setTypeParameters(klass)
            val fieldsForContextReceiversOfCurrentClass = createContextReceiverFields(klass)
            if (fieldsForContextReceiversOfCurrentClass.isNotEmpty()) {
                declarations.addAll(fieldsForContextReceiversOfCurrentClass)
                fieldsForContextReceivers[this] = fieldsForContextReceiversOfCurrentClass
            }
        }
    }

    fun IrClass.createContextReceiverFields(klass: FirRegularClass): List<IrField> {
        if (klass.contextReceivers.isEmpty()) return emptyList()

        val contextReceiverFields = mutableListOf<IrField>()
        for ((index, contextReceiver) in klass.contextReceivers.withIndex()) {
            val irField = components.irFactory.createField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.FIELD_FOR_CLASS_CONTEXT_RECEIVER,
                IrFieldSymbolImpl(),
                Name.identifier("contextReceiverField$index"),
                contextReceiver.typeRef.toIrType(),
                DescriptorVisibilities.PRIVATE,
                isFinal = true,
                isExternal = false,
                isStatic = false
            )
            irField.parent = this@createContextReceiverFields
            contextReceiverFields.add(irField)
        }

        return contextReceiverFields
    }

    fun getFieldsWithContextReceiversForClass(irClass: IrClass): List<IrField>? = fieldsForContextReceivers[irClass]

    private fun IrClass.declareSupertypes(klass: FirClass) {
        superTypes = klass.superTypeRefs.map { superTypeRef -> superTypeRef.toIrType() }
    }

    private fun IrClass.declareValueClassRepresentation(klass: FirRegularClass) {
        if (this !is Fir2IrLazyClass) {
            valueClassRepresentation = computeValueClassRepresentation(klass)
        }
    }

    private fun IrClass.declareSupertypesAndTypeParameters(klass: FirClass): IrClass {
        declareTypeParameters(klass)
        declareSupertypes(klass)
        return this
    }

    fun getCachedIrClass(klass: FirClass): IrClass? {
        return if (klass is FirAnonymousObject || klass is FirRegularClass && klass.visibility == Visibilities.Local) {
            localStorage[klass]
        } else {
            classCache[klass]
        }
    }

    private fun getCachedLocalClass(lookupTag: ConeClassLikeLookupTag): IrClass? {
        return localStorage[lookupTag.toSymbol(session)!!.fir as FirClass]
    }

    private fun FirRegularClass.enumClassModality(): Modality {
        return when {
            declarations.any { it is FirCallableDeclaration && it.modality == Modality.ABSTRACT } -> {
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

    // This function is called when we refer local class earlier than we reach its declaration
    // This can happen e.g. when implicit return type has a local class constructor
    private fun createLocalIrClassOnTheFly(klass: FirClass): IrClass {
        // finding the parent class that actually contains the [klass] in the tree - it is the root one that should be created on the fly
        val classOrLocalParent = generateSequence(klass) { c ->
            (c as? FirRegularClass)?.containingClassForLocalAttr?.let { lookupTag ->
                (firProvider.symbolProvider.getSymbolByLookupTag(lookupTag)?.fir as? FirClass)?.takeIf {
                    it.declarations.contains(c)
                }
            }
        }.last()
        val result = when (classOrLocalParent) {
            is FirAnonymousObject -> createIrAnonymousObject(classOrLocalParent, irParent = temporaryParent).apply {
                converter.processAnonymousObjectOnTheFly(classOrLocalParent, this)
            }
            is FirRegularClass -> {
                converter.processLocalClassAndNestedClassesOnTheFly(classOrLocalParent, temporaryParent)
            }
        }
        // Note: usually member creation and f/o binding is delayed till non-local classes are processed in Fir2IrConverter
        // If non-local classes are already created (this means we are in body translation) we do everything immediately
        // The last variant is possible for local variables like 'val a = object : Any() { ... }'
        if (processMembersOfClassesOnTheFlyImmediately) {
            processMembersOfClassCreatedOnTheFly(classOrLocalParent, result)
            converter.bindFakeOverridesInClass(result)
        } else {
            localClassesCreatedOnTheFly[classOrLocalParent] = result
        }
        return if (classOrLocalParent === klass) result
        else (getCachedIrClass(klass)
            ?: error("Assuming that all nested classes of ${classOrLocalParent.classId.asString()} should already be cached"))
    }

    // Note: this function is called exactly once, right after Fir2IrConverter finished f/o binding for regular classes
    fun processMembersOfClassesCreatedOnTheFly() {
        // After the call of this function, members of local classes may be processed immediately
        // Before the call it's not possible, because f/o binding for regular classes isn't done yet
        processMembersOfClassesOnTheFlyImmediately = true
        for ((klass, irClass) in localClassesCreatedOnTheFly) {
            processMembersOfClassCreatedOnTheFly(klass, irClass)
        }
        // Note: it's better to bind everything AFTER members are built, in case local classes are dependent on each other
        localClassesCreatedOnTheFly.values.forEach(converter::bindFakeOverridesInClass)
        localClassesCreatedOnTheFly.clear()
    }

    private fun processMembersOfClassCreatedOnTheFly(klass: FirClass, irClass: IrClass) {
        when (klass) {
            is FirRegularClass -> converter.processClassMembers(klass, irClass)
            is FirAnonymousObject -> converter.processAnonymousObjectMembers(klass, irClass, processHeaders = false)
        }
    }

    fun processClassHeader(regularClass: FirRegularClass, irClass: IrClass = getCachedIrClass(regularClass)!!): IrClass {
        irClass.declareTypeParameters(regularClass)
        irClass.setThisReceiver(regularClass.typeParameters)
        irClass.declareSupertypes(regularClass)
        irClass.declareValueClassRepresentation(regularClass)
        return irClass
    }

    private fun declareIrTypeAlias(signature: IdSignature?, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        if (signature == null)
            factory(IrTypeAliasSymbolImpl())
        else
            symbolTable.declareTypeAlias(signature, { Fir2IrTypeAliasSymbol(signature) }, factory)

    fun registerTypeAlias(
        typeAlias: FirTypeAlias,
        parent: IrFile
    ): IrTypeAlias {
        val signature = signatureComposer.composeSignature(typeAlias)
        return typeAlias.convertWithOffsets { startOffset, endOffset ->
            declareIrTypeAlias(signature) { symbol ->
                preCacheTypeParameters(typeAlias, symbol)
                val irTypeAlias = irFactory.createTypeAlias(
                    startOffset, endOffset, symbol,
                    typeAlias.name, components.visibilityConverter.convertToDescriptorVisibility(typeAlias.visibility),
                    typeAlias.expandedTypeRef.toIrType(),
                    typeAlias.isActual, IrDeclarationOrigin.DEFINED
                ).apply {
                    this.parent = parent
                    setTypeParameters(typeAlias)
                    parent.declarations += this
                }
                typeAliasCache[typeAlias] = irTypeAlias
                irTypeAlias
            }
        }
    }

    internal fun getCachedTypeAlias(firTypeAlias: FirTypeAlias): IrTypeAlias? = typeAliasCache[firTypeAlias]

    private fun declareIrClass(signature: IdSignature?, factory: (IrClassSymbol) -> IrClass): IrClass =
        if (signature == null)
            factory(IrClassSymbolImpl())
        else
            symbolTable.declareClass(signature, { Fir2IrClassSymbol(signature) }, factory)

    fun registerIrClass(
        regularClass: FirRegularClass,
        parent: IrDeclarationParent? = null,
        predefinedOrigin: IrDeclarationOrigin? = null
    ): IrClass {
        val visibility = regularClass.visibility
        val modality = when (regularClass.classKind) {
            ClassKind.ENUM_CLASS -> regularClass.enumClassModality()
            ClassKind.ANNOTATION_CLASS -> Modality.OPEN
            else -> regularClass.modality ?: Modality.FINAL
        }
        val signature = runUnless(regularClass.isLocal || !generateSignatures) {
            signatureComposer.composeSignature(regularClass)
        }
        val irClass = regularClass.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { symbol ->
                irFactory.createClass(
                    startOffset,
                    endOffset,
                    regularClass.computeIrOrigin(predefinedOrigin),
                    symbol,
                    regularClass.name,
                    regularClass.classKind,
                    components.visibilityConverter.convertToDescriptorVisibility(visibility),
                    modality,
                    isCompanion = regularClass.isCompanion,
                    isInner = regularClass.isInner,
                    isData = regularClass.isData,
                    isExternal = regularClass.isExternal,
                    isValue = regularClass.isInline,
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
        if (regularClass.visibility == Visibilities.Local) {
            localStorage[regularClass] = irClass
        } else {
            classCache[regularClass] = irClass
        }
        return irClass
    }

    fun createIrAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.Local,
        name: Name = Name.special("<no name provided>"),
        irParent: IrDeclarationParent? = null
    ): IrClass {
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        val result = anonymousObject.convertWithOffsets { startOffset, endOffset ->
            irFactory.createClass(
                startOffset, endOffset, origin, IrClassSymbolImpl(), name,
                // NB: for unknown reason, IR uses 'CLASS' kind for simple anonymous objects
                anonymousObject.classKind.takeIf { it == ClassKind.ENUM_ENTRY } ?: ClassKind.CLASS,
                components.visibilityConverter.convertToDescriptorVisibility(visibility), modality
            ).apply {
                metadata = FirMetadataSource.Class(anonymousObject)
                setThisReceiver(anonymousObject.typeParameters)
                if (irParent != null) {
                    this.parent = irParent
                }
            }
        }.declareSupertypesAndTypeParameters(anonymousObject)
        localStorage[anonymousObject] = result
        return result
    }

    private fun getIrAnonymousObjectForEnumEntry(anonymousObject: FirAnonymousObject, name: Name, irParent: IrClass?): IrClass {
        localStorage[anonymousObject]?.let { return it }
        return createIrAnonymousObject(anonymousObject, Visibilities.Private, name, irParent)
    }

    private fun createIrTypeParameterWithoutBounds(
        typeParameter: FirTypeParameter,
        index: Int,
        ownerSymbol: IrSymbol,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
    ): IrTypeParameter {
        require(index >= 0)
        val origin = typeParameter.computeIrOrigin()
        val irTypeParameter = with(typeParameter) {
            convertWithOffsets { startOffset, endOffset ->
                signatureComposer.composeTypeParameterSignature(
                    typeParameter, index, ownerSymbol.signature
                )?.let { signature ->
                    if (ownerSymbol is IrClassifierSymbol) {
                        symbolTable.declareGlobalTypeParameter(
                            signature,
                            symbolFactory = { IrTypeParameterPublicSymbolImpl(signature) }
                        ) { symbol ->
                            irFactory.createTypeParameter(
                                startOffset, endOffset, origin, symbol,
                                name, if (index < 0) 0 else index,
                                isReified,
                                variance
                            )
                        }
                    } else {
                        symbolTable.declareScopedTypeParameter(
                            signature,
                            symbolFactory = { IrTypeParameterPublicSymbolImpl(signature) }
                        ) { symbol ->
                            irFactory.createTypeParameter(
                                startOffset, endOffset, origin, symbol,
                                name, if (index < 0) 0 else index,
                                isReified,
                                variance
                            )
                        }

                    }
                } ?: irFactory.createTypeParameter(
                    startOffset, endOffset, origin, IrTypeParameterSymbolImpl(),
                    name, if (index < 0) 0 else index,
                    isReified,
                    variance
                )
            }
        }

        // Cache the type parameter BEFORE processing its bounds/supertypes, to properly handle recursive type bounds.
        if (typeContext.origin == ConversionTypeOrigin.SETTER) {
            typeParameterCacheForSetter[typeParameter] = irTypeParameter
        } else {
            typeParameterCache[typeParameter] = irTypeParameter
        }
        annotationGenerator.generate(irTypeParameter, typeParameter)
        return irTypeParameter
    }

    internal fun getCachedIrTypeParameter(
        typeParameter: FirTypeParameter,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter? {
        return if (typeContext.origin == ConversionTypeOrigin.SETTER)
            typeParameterCacheForSetter[typeParameter]
        else
            typeParameterCache[typeParameter]
    }

    internal fun getIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int,
        ownerSymbol: IrSymbol,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter {
        getCachedIrTypeParameter(typeParameter, typeContext)?.let { return it }
        return typeParameter.run {
            val irTypeParameter = createIrTypeParameterWithoutBounds(typeParameter, index, ownerSymbol, typeContext)
            irTypeParameter.superTypes = bounds.map { it.toIrType() }
            irTypeParameter
        }
    }

    fun putEnumEntryClassInScope(enumEntry: FirEnumEntry, correspondingClass: IrClass) {
        localStorage[(enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject] = correspondingClass
    }

    internal fun getCachedIrEnumEntry(enumEntry: FirEnumEntry): IrEnumEntry? = enumEntryCache[enumEntry]

    private fun declareIrEnumEntry(signature: IdSignature?, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry =
        if (signature == null)
            factory(IrEnumEntrySymbolImpl())
        else
            symbolTable.declareEnumEntry(signature, { Fir2IrEnumEntrySymbol(signature) }, factory)

    fun getIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        forceTopLevelPrivate: Boolean = false,
    ): IrEnumEntry {
        getCachedIrEnumEntry(enumEntry)?.let { return it }
        val containingFile = firProvider.getFirCallableContainerFile(enumEntry.symbol)
        val irParentClass = irParent ?: enumEntry.containingClassLookupTag()?.let { findIrClass(it) }

        @Suppress("NAME_SHADOWING")
        val predefinedOrigin = predefinedOrigin ?: if (containingFile != null) {
            IrDeclarationOrigin.DEFINED
        } else {
            irParentClass?.origin ?: IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        }
        return createIrEnumEntry(
            enumEntry,
            irParent = irParentClass,
            predefinedOrigin = predefinedOrigin,
            forceTopLevelPrivate
        )
    }

    fun findIrClass(lookupTag: ConeClassLikeLookupTag): IrClass? {
        return if (lookupTag.classId.isLocal) {
            getCachedLocalClass(lookupTag)
        } else {
            val firSymbol = lookupTag.toSymbol(session)
            if (firSymbol is FirClassSymbol) {
                getIrClassSymbol(firSymbol).owner
            } else {
                null
            }
        }
    }

    fun createIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        forceTopLevelPrivate: Boolean = false,
    ): IrEnumEntry {
        return enumEntry.convertWithOffsets { startOffset, endOffset ->
            val signature = signatureComposer.composeSignature(enumEntry, forceTopLevelPrivate = forceTopLevelPrivate)
            if (signature != null) {
                // Compilation of kotlinx-serialization-protobuf fails with "already bound" exception if this check is removed
                // TODO: get rid of this check
                symbolTable.referenceEnumEntryIfAny(signature)?.let { if (it.isBound) return@convertWithOffsets it.owner }
            }
            val result = declareIrEnumEntry(signature) { symbol ->
                val origin = enumEntry.computeIrOrigin(predefinedOrigin)
                irFactory.createEnumEntry(
                    startOffset, endOffset, origin, symbol, enumEntry.name
                ).apply {
                    declarationStorage.enterScope(this)
                    if (irParent != null) {
                        this.parent = irParent
                    }
                    val initializer = enumEntry.initializer
                    if (initializer is FirAnonymousObjectExpression) {
                        // An enum entry with its own members
                        if (initializer.anonymousObject.declarations.any { it !is FirConstructor }) {
                            val klass = getIrAnonymousObjectForEnumEntry(initializer.anonymousObject, enumEntry.name, irParent)
                            this.correspondingClass = klass
                        }
                        // Otherwise, this is a default-ish enum entry whose initializer would be a delegating constructor call,
                        // which will be translated via visitor later.
                    }
                    declarationStorage.leaveScope(this)
                }
            }
            enumEntryCache[enumEntry] = result
            result
        }
    }

    fun getIrClassSymbol(firClassSymbol: FirClassSymbol<*>, forceTopLevelPrivate: Boolean = false): IrClassSymbol {
        val firClass = firClassSymbol.fir
        getCachedIrClass(firClass)?.let { return it.symbol }
        if (firClass is FirAnonymousObject || firClass is FirRegularClass && firClass.visibility == Visibilities.Local) {
            return createLocalIrClassOnTheFly(firClass).symbol
        }
        firClass as FirRegularClass
        val classId = firClassSymbol.classId
        val parentId = classId.outerClassId
        val parentClass = parentId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) }
        val irParent = declarationStorage.findIrParent(classId.packageFqName, parentClass?.toLookupTag(), firClassSymbol, firClass.origin)!!

        // firClass may be referenced by some parent's type parameters as a bound. In that case, getIrClassSymbol will be called recursively.
        getCachedIrClass(firClass)?.let { return it.symbol }

        val signature = runIf(generateSignatures) {
            signatureComposer.composeSignature(firClass, forceTopLevelPrivate = forceTopLevelPrivate)
        }
        val irClass = firClass.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { irClassSymbol ->
                Fir2IrLazyClass(components, startOffset, endOffset, firClass.irOrigin(firProvider), firClass, irClassSymbol).apply {
                    parent = irParent
                }
            }
        }
        classCache[firClass] = irClass
        // NB: this is needed to prevent recursions in case of self bounds
        (irClass as Fir2IrLazyClass).prepareTypeParameters()

        return irClass.symbol
    }

    fun getIrClassSymbolForNotFoundClass(classLikeLookupTag: ConeClassLikeLookupTag): IrClassSymbol {
        val classId = classLikeLookupTag.classId
        val signature = IdSignature.CommonSignature(
            classId.packageFqName.asString(), classId.relativeClassName.asString(), 0, 0,
        )

        val parentId = classId.outerClassId
        val parentClass = parentId?.let { getIrClassSymbolForNotFoundClass(ConeClassLikeLookupTagImpl(it)) }
        val irParent = parentClass?.owner ?: declarationStorage.getIrExternalPackageFragment(classId.packageFqName)

        val symbol = Fir2IrClassSymbol(signature)
        symbolTable.declareClass(signature, { symbol }) {
            irFactory.createClass(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, symbol, classId.shortClassName,
                ClassKind.CLASS, DescriptorVisibilities.DEFAULT_VISIBILITY, Modality.FINAL,
            ).apply {
                parent = irParent
            }
        }

        return symbol
    }

    fun getIrTypeParameterSymbol(
        firTypeParameterSymbol: FirTypeParameterSymbol,
        typeContext: ConversionTypeContext
    ): IrTypeParameterSymbol {
        val firTypeParameter = firTypeParameterSymbol.fir
        return getCachedIrTypeParameter(firTypeParameter, typeContext)?.symbol
        // We can try to use default cache because setter can use parent type parameters
            ?: typeParameterCache[firTypeParameter]?.symbol
            ?: error("Cannot find cached type parameter by FIR symbol: ${firTypeParameterSymbol.name}")
    }

    private val temporaryParent by lazy {
        irFactory.createFunction(
            startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED, IrSimpleFunctionSymbolImpl(),
            Name.special("<stub>"), DescriptorVisibilities.PRIVATE, Modality.FINAL, irBuiltIns.unitType,
            isInline = false, isExternal = false, isTailrec = false,
            isSuspend = false, isOperator = false, isInfix = false, isExpect = false
        ).apply {
            parent = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName.ROOT)
        }
    }
}
