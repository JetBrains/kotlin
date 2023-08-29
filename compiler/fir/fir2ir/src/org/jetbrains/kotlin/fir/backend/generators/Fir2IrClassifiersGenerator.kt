/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class Fir2IrClassifiersGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {
    private val localClassesCreatedOnTheFly: MutableMap<FirClass, IrClass> = mutableMapOf()

    private var processMembersOfClassesOnTheFlyImmediately = false

    internal fun setTypeParameters(
        irOwner: IrTypeParametersContainer,
        owner: FirTypeParameterRefsOwner,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ) {
        irOwner.typeParameters = owner.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index, irOwner.symbol, typeOrigin).apply {
                parent = irOwner
                if (superTypes.isEmpty()) {
                    superTypes = typeParameter.bounds.map { it.toIrType(typeOrigin) }
                }
            }
        }
    }

    private fun IrClass.declareTypeParameters(klass: FirClass) {
        classifierStorage.preCacheTypeParameters(klass, symbol)
        setTypeParameters(this, klass)
        if (klass is FirRegularClass) {
            val fieldsForContextReceiversOfCurrentClass = classifierStorage.getFieldsWithContextReceiversForClass(this, klass)
            declarations.addAll(fieldsForContextReceiversOfCurrentClass)
        }
    }

    private fun IrClass.declareSupertypes(klass: FirClass) {
        superTypes = klass.superTypeRefs.map { superTypeRef -> superTypeRef.toIrType() }
    }

    private fun IrClass.declareValueClassRepresentation(klass: FirRegularClass) {
        if (this !is Fir2IrLazyClass) {
            valueClassRepresentation = computeValueClassRepresentation(klass)
        }
    }

    private fun FirRegularClass.enumClassModality(): Modality {
        return when {
            declarations.any { it is FirCallableDeclaration && it.modality == Modality.ABSTRACT } -> {
                Modality.ABSTRACT
            }
            declarations.none {
                it is FirEnumEntry && isEnumEntryWhichRequiresSubclass(it)
            } -> {
                Modality.FINAL
            }
            hasAbstractMembersInScope() -> {
                Modality.ABSTRACT
            }
            else -> {
                Modality.OPEN
            }
        }
    }

    private fun FirRegularClass.hasAbstractMembersInScope(): Boolean {
        val scope = unsubstitutedScope()
        val names = scope.getCallableNames()
        var hasAbstract = false
        for (name in names) {
            scope.processFunctionsByName(name) {
                if (it.isAbstract) {
                    hasAbstract = true
                }
            }
            if (hasAbstract) return true
            scope.processPropertiesByName(name) {
                if (it.isAbstract) {
                    hasAbstract = true
                }
            }
            if (hasAbstract) return true
        }
        return false
    }

    // TODO: it's worth to move this onTheFly stuff back to classifier storage
    // This function is called when we refer local class earlier than we reach its declaration
    // This can happen e.g. when implicit return type has a local class constructor
    private fun createLocalIrClassOnTheFly(klass: FirClass): IrClass {
        // finding the parent class that actually contains the [klass] in the tree - it is the root one that should be created on the fly
        val classOrLocalParent = generateSequence(klass) { c ->
            (c as? FirRegularClass)?.containingClassForLocalAttr?.let { lookupTag ->
                (session.firProvider.symbolProvider.getSymbolByLookupTag(lookupTag)?.fir as? FirClass)?.takeIf {
                    it.declarations.contains(c)
                }
            }
        }.last()
        val result = converter.processLocalClassAndNestedClassesOnTheFly(classOrLocalParent, temporaryParent)
        // Note: usually member creation and f/o binding is delayed till non-local classes are processed in Fir2IrConverter
        // If non-local classes are already created (this means we are in body translation) we do everything immediately
        // The last variant is possible for local variables like 'val a = object : Any() { ... }'
        if (processMembersOfClassesOnTheFlyImmediately) {
            converter.processClassMembers(classOrLocalParent, result)
            converter.bindFakeOverridesInClass(result)
        } else {
            localClassesCreatedOnTheFly[classOrLocalParent] = result
        }
        return if (classOrLocalParent === klass) result
        else (classifierStorage.getCachedIrClass(klass)
            ?: error("Assuming that all nested classes of ${classOrLocalParent.classId.asString()} should already be cached"))
    }

    // Note: this function is called exactly once, right after Fir2IrConverter finished f/o binding for regular classes
    fun processMembersOfClassesCreatedOnTheFly() {
        // After the call of this function, members of local classes may be processed immediately
        // Before the call it's not possible, because f/o binding for regular classes isn't done yet
        processMembersOfClassesOnTheFlyImmediately = true
        for ((klass, irClass) in localClassesCreatedOnTheFly) {
            converter.processClassMembers(klass, irClass)
            // See the problem from KT-57441
//            class Wrapper {
//                private val dummy = object: Bar {}
//                private val bar = object: Bar by dummy {}
//            }
//            interface Bar {
//                val foo: String
//                    get() = ""
//            }
            // When we are building bar.foo fake override, we should call dummy.foo,
            // so we should have object : Bar.foo fake override to be built and bound.
            converter.bindFakeOverridesInClass(irClass)
        }
        localClassesCreatedOnTheFly.clear()
    }

    fun processClassHeader(klass: FirClass, irClass: IrClass = classifierStorage.getCachedIrClass(klass)!!): IrClass {
        irClass.declareTypeParameters(klass)
        irClass.setThisReceiver(klass.typeParameters)
        irClass.declareSupertypes(klass)
        if (klass is FirRegularClass) {
            irClass.declareValueClassRepresentation(klass)
        }
        return irClass
    }

    private fun IrClass.setThisReceiver(typeParameters: List<FirTypeParameterRef>) {
        symbolTable.enterScope(this)
        val typeArguments = typeParameters.map {
            IrSimpleTypeImpl(classifierStorage.getIrTypeParameterSymbol(it.symbol, ConversionTypeOrigin.DEFAULT), false, emptyList(), emptyList())
        }
        thisReceiver = declareThisReceiverParameter(
            thisType = IrSimpleTypeImpl(symbol, false, typeArguments, emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
    }


    private fun declareIrTypeAlias(signature: IdSignature?, factory: (IrTypeAliasSymbol) -> IrTypeAlias): IrTypeAlias =
        if (signature == null)
            factory(IrTypeAliasSymbolImpl())
        else
            symbolTable.declareTypeAlias(signature, { Fir2IrTypeAliasSymbol(signature) }, factory)

    fun createIrTypeAlias(
        typeAlias: FirTypeAlias,
        parent: IrDeclarationParent
    ): IrTypeAlias = typeAlias.convertWithOffsets { startOffset, endOffset ->
        val signature = signatureComposer.composeSignature(typeAlias)
        declareIrTypeAlias(signature) { symbol ->
            classifierStorage.preCacheTypeParameters(typeAlias, symbol)
            val irTypeAlias = irFactory.createTypeAlias(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = IrDeclarationOrigin.DEFINED,
                name = typeAlias.name,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(typeAlias.visibility),
                symbol = symbol,
                isActual = typeAlias.isActual,
                expandedType = typeAlias.expandedTypeRef.toIrType(),
            ).apply {
                this.parent = parent
                setTypeParameters(this, typeAlias)
                if (parent is IrFile) {
                    parent.declarations += this
                }
            }
            irTypeAlias
        }
    }

    fun declareIrClass(signature: IdSignature?, factory: (IrClassSymbol) -> IrClass): IrClass {
        return if (signature == null)
            factory(IrClassSymbolImpl())
        else
            symbolTable.declareClass(signature, { Fir2IrClassSymbol(signature) }, factory)
    }

    fun createIrClass(
        regularClass: FirRegularClass,
        parent: IrDeclarationParent,
        predefinedOrigin: IrDeclarationOrigin? = null
    ): IrClass {
        val visibility = regularClass.visibility
        val modality = when (regularClass.classKind) {
            ClassKind.ENUM_CLASS -> regularClass.enumClassModality()
            ClassKind.ANNOTATION_CLASS -> Modality.OPEN
            else -> regularClass.modality ?: Modality.FINAL
        }
        val signature = runUnless(regularClass.isLocal || !configuration.linkViaSignatures) {
            signatureComposer.composeSignature(regularClass)
        }
        val irClass = regularClass.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { symbol ->
                irFactory.createClass(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = regularClass.computeIrOrigin(predefinedOrigin),
                    name = regularClass.name,
                    visibility = components.visibilityConverter.convertToDescriptorVisibility(visibility),
                    symbol = symbol,
                    kind = regularClass.classKind,
                    modality = modality,
                    isExternal = regularClass.isExternal,
                    isCompanion = regularClass.isCompanion,
                    isInner = regularClass.isInner,
                    isData = regularClass.isData,
                    isValue = regularClass.isInline,
                    isExpect = regularClass.isExpect,
                    isFun = regularClass.isFun
                ).apply {
                    metadata = FirMetadataSource.Class(regularClass)
                }
            }
        }
        irClass.parent = parent
        return irClass
    }

    fun createAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.Local,
        name: Name = SpecialNames.NO_NAME_PROVIDED,
        irParent: IrDeclarationParent? = null
    ): IrClass {
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        val irAnonymousObject = anonymousObject.convertWithOffsets { startOffset, endOffset ->
            irFactory.createClass(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(visibility),
                symbol = IrClassSymbolImpl(),
                kind = anonymousObject.classKind,
                modality = modality,
            ).apply {
                metadata = FirMetadataSource.Class(anonymousObject)
            }
        }
        if (irParent != null) {
            irAnonymousObject.parent = irParent
        }
        return irAnonymousObject
    }

    fun createCodeFragmentClass(codeFragment: FirCodeFragment, containingFile: IrFile): IrClass {
        val conversionData = codeFragment.conversionData
        val signature = signatureComposer.composeSignature(codeFragment)

        val irClass = codeFragment.convertWithOffsets { startOffset, endOffset ->
            declareIrClass(signature) { symbol ->
                irFactory.createClass(
                    startOffset,
                    endOffset,
                    IrDeclarationOrigin.DEFINED,
                    conversionData.classId.shortClassName,
                    DescriptorVisibilities.PUBLIC,
                    symbol,
                    ClassKind.CLASS,
                    Modality.FINAL,
                    isExternal = false,
                    isCompanion = false,
                    isInner = false,
                    isData = false,
                    isValue = false,
                    isExpect = false,
                    isFun = false
                ).apply {
                    metadata = FirMetadataSource.CodeFragment(codeFragment)
                    parent = containingFile
                    typeParameters = emptyList()
                    thisReceiver = declareThisReceiverParameter(
                        thisType = IrSimpleTypeImpl(symbol, false, emptyList(), emptyList()),
                        thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
                    )
                    superTypes = listOf(irBuiltIns.anyType)
                }
            }
        }
        return irClass
    }

    fun initializeTypeParameterBounds(typeParameter: FirTypeParameter, irTypeParameter: IrTypeParameter) {
        irTypeParameter.superTypes = typeParameter.bounds.map { it.toIrType() }
    }

    fun createIrTypeParameterWithoutBounds(
        typeParameter: FirTypeParameter,
        index: Int,
        ownerSymbol: IrSymbol,
    ): IrTypeParameter {
        require(index >= 0)
        val origin = typeParameter.computeIrOrigin()
        val irTypeParameter = with(typeParameter) {
            convertWithOffsets { startOffset, endOffset ->
                signatureComposer.composeTypeParameterSignature(
                    index, ownerSymbol.signature
                )?.let { signature ->
                    if (ownerSymbol is IrClassifierSymbol) {
                        symbolTable.declareGlobalTypeParameter(
                            signature,
                            symbolFactory = { IrTypeParameterPublicSymbolImpl(signature) }
                        ) { symbol ->
                            irFactory.createTypeParameter(
                                startOffset = startOffset,
                                endOffset = endOffset,
                                origin = origin,
                                name = name,
                                symbol = symbol,
                                variance = variance,
                                index = if (index < 0) 0 else index,
                                isReified = isReified,
                            )
                        }
                    } else {
                        symbolTable.declareScopedTypeParameter(
                            signature,
                            symbolFactory = { IrTypeParameterPublicSymbolImpl(signature) }
                        ) { symbol ->
                            irFactory.createTypeParameter(
                                startOffset = startOffset,
                                endOffset = endOffset,
                                origin = origin,
                                name = name,
                                symbol = symbol,
                                variance = variance,
                                index = if (index < 0) 0 else index,
                                isReified = isReified,
                            )
                        }

                    }
                } ?: irFactory.createTypeParameter(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = origin,
                    name = name,
                    symbol = IrTypeParameterSymbolImpl(),
                    variance = variance,
                    index = if (index < 0) 0 else index,
                    isReified = isReified,
                )
            }
        }
        annotationGenerator.generate(irTypeParameter, typeParameter)
        return irTypeParameter
    }

    private fun declareIrEnumEntry(signature: IdSignature?, factory: (IrEnumEntrySymbol) -> IrEnumEntry): IrEnumEntry {
        return if (signature == null)
            factory(IrEnumEntrySymbolImpl())
        else
            symbolTable.declareEnumEntry(signature, { Fir2IrEnumEntrySymbol(signature) }, factory)
    }

    fun createIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass?,
        predefinedOrigin: IrDeclarationOrigin? = null,
    ): IrEnumEntry {
        return enumEntry.convertWithOffsets { startOffset, endOffset ->
            val signature = signatureComposer.composeSignature(enumEntry)
            declareIrEnumEntry(signature) { symbol ->
                val origin = enumEntry.computeIrOrigin(predefinedOrigin)
                irFactory.createEnumEntry(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = origin,
                    name = enumEntry.name,
                    symbol = symbol,
                ).apply {
                    declarationStorage.enterScope(this.symbol)
                    if (irParent != null) {
                        this.parent = irParent
                    }
                    if (isEnumEntryWhichRequiresSubclass(enumEntry)) {
                        // An enum entry with its own members requires an anonymous object generated.
                        // Otherwise, this is a default-ish enum entry whose initializer would be a delegating constructor call,
                        // which will be translated via visitor later.
                        val klass = classifierStorage.getIrAnonymousObjectForEnumEntry(
                            (enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject, enumEntry.name, irParent
                        )
                        this.correspondingClass = klass
                    }
                    declarationStorage.leaveScope(this.symbol)
                }
            }
        }
    }

    private fun isEnumEntryWhichRequiresSubclass(enumEntry: FirEnumEntry): Boolean {
        val initializer = enumEntry.initializer
        return initializer is FirAnonymousObjectExpression && initializer.anonymousObject.declarations.any { it !is FirConstructor }
    }

    @OptIn(IrSymbolInternals::class)
    fun createIrClassSymbolForNotFoundClass(classLikeLookupTag: ConeClassLikeLookupTag): IrClassSymbol {
        val classId = classLikeLookupTag.classId
        val signature = IdSignature.CommonSignature(
            packageFqName = classId.packageFqName.asString(),
            declarationFqName = classId.relativeClassName.asString(),
            id = 0,
            mask = 0,
            description = null,
        )

        val parentId = classId.outerClassId
        val parentClass = parentId?.let { createIrClassSymbolForNotFoundClass(it.toLookupTag()) }
        val irParent = parentClass?.owner ?: declarationStorage.getIrExternalPackageFragment(classId.packageFqName)

        return symbolTable.referenceClass(signature, { Fir2IrClassSymbol(signature) }) {
            irFactory.createClass(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = classId.shortClassName,
                visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
                symbol = it,
                kind = ClassKind.CLASS,
                modality = Modality.FINAL,
            ).apply {
                parent = irParent
            }
        }
    }

    private val temporaryParent by lazy {
        irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.special("<stub>"),
            visibility = DescriptorVisibilities.PRIVATE,
            isInline = false,
            isExpect = false,
            returnType = irBuiltIns.unitType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).apply {
            parent = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName.ROOT)
        }
    }
}
