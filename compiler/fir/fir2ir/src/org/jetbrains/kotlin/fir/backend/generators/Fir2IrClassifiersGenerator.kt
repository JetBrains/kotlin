/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.hasEnumEntries
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.bindSymbolToLookupTag
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.name.*

class Fir2IrClassifiersGenerator(private val c: Fir2IrComponents) : Fir2IrComponents by c {
    // ------------------------------------ type parameters ------------------------------------

    fun createIrTypeParameterWithoutBounds(
        typeParameter: FirTypeParameter,
        index: Int,
        symbol: IrTypeParameterSymbol
    ): IrTypeParameter {
        require(index >= 0)
        val origin = typeParameter.computeIrOrigin()
        val irTypeParameter = typeParameter.convertWithOffsets { startOffset, endOffset ->
            IrFactoryImpl.createTypeParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = typeParameter.name,
                symbol = symbol,
                variance = typeParameter.variance,
                index = index,
                isReified = typeParameter.isReified,
            )
        }
        annotationGenerator.generate(irTypeParameter, typeParameter)
        return irTypeParameter
    }

    fun initializeTypeParameterBounds(typeParameter: FirTypeParameter, irTypeParameter: IrTypeParameter) {
        irTypeParameter.superTypes = typeParameter.bounds.map { it.toIrType(c) }
    }

    // ------------------------------------ classes ------------------------------------

    fun createIrClass(
        regularClass: FirRegularClass,
        parent: IrDeclarationParent,
        symbol: IrClassSymbol,
        predefinedOrigin: IrDeclarationOrigin? = null
    ): IrClass {
        val visibility = regularClass.visibility
        val modality = when (regularClass.classKind) {
            ClassKind.ENUM_CLASS -> regularClass.enumClassModality()
            ClassKind.ANNOTATION_CLASS -> Modality.OPEN
            else -> regularClass.modality ?: Modality.FINAL
        }
        val irClass = regularClass.convertWithOffsets { startOffset, endOffset ->
            IrFactoryImpl.createClass(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = regularClass.computeIrOrigin(predefinedOrigin),
                name = regularClass.name,
                visibility = c.visibilityConverter.convertToDescriptorVisibility(visibility),
                symbol = symbol,
                kind = regularClass.classKind,
                modality = modality,
                isExternal = isEffectivelyExternal(regularClass, parent),
                isCompanion = regularClass.isCompanion,
                isInner = regularClass.isInner,
                isData = regularClass.isData,
                isValue = regularClass.isInlineOrValue,
                isExpect = regularClass.isExpect,
                isFun = regularClass.isFun,
                hasEnumEntries = regularClass.hasEnumEntries,
            ).apply {
                metadata = FirMetadataSource.Class(regularClass)
            }
        }
        /*
         * `regularClass.isLocal` indicates that either class itsef is local or it is a nested class in some other class
         * Check for parentClassId allows to distinguish between those cases
         */
        irClass.setParent(parent)
        if (!(regularClass.isLocal && regularClass.classId.parentClassId == null)) {
            addDeclarationToParent(irClass, parent)
        }
        return irClass
    }

    fun processClassHeader(klass: FirClass, irClass: IrClass = classifierStorage.getIrClass(klass)): IrClass {
        irClass.declareTypeParameters(klass)
        irClass.setThisReceiver(c, klass.typeParameters)
        irClass.declareSupertypes(klass)
        if (klass is FirRegularClass) {
            irClass.declareValueClassRepresentation(klass)
        }
        return irClass
    }

    // `irClass` is a source class and definitely is not a lazy class
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.declareTypeParameters(klass: FirClass) {
        classifierStorage.preCacheTypeParameters(klass)
        setTypeParameters(this, klass)
        if (klass is FirRegularClass) {
            val fieldsForContextReceiversOfCurrentClass = classifierStorage.getFieldsWithContextReceiversForClass(this, klass)
            declarations.addAll(fieldsForContextReceiversOfCurrentClass)
        }
    }

    private fun IrClass.declareSupertypes(klass: FirClass) {
        superTypes = klass.superTypeRefs.map { superTypeRef -> superTypeRef.toIrType(c) }
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
        val scope = unsubstitutedScope(c)
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

    // ------------------------------------ local classes ------------------------------------

    data class LocalIrClassInfo(
        val irClass: IrClass,
        val firClassOrLocalParent: FirClass,
        val irClassOrLocalParent: IrClass,
    )

    // This function is called when we refer local class earlier than we reach its declaration
    // This can happen e.g. when implicit return type has a local class constructor
    fun createLocalIrClassOnTheFly(klass: FirClass, processMembersOfClassesOnTheFlyImmediately: Boolean): LocalIrClassInfo {
        // finding the parent class that actually contains the [klass] in the tree - it is the root one that should be created on the fly
        val classOrLocalParent = generateSequence(klass) { c ->
            (c as? FirRegularClass)?.containingClassForLocalAttr?.let { lookupTag ->
                (lookupTag.toClassSymbol(session)?.fir)?.takeIf {
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
        }
        val irClass = if (classOrLocalParent === klass) {
            result
        } else {
            classifierStorage.getIrClass(klass)
        }
        return LocalIrClassInfo(irClass, classOrLocalParent, result)
    }

    private val temporaryParent by lazy {
        IrFactoryImpl.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.special("<stub>"),
            visibility = DescriptorVisibilities.PRIVATE,
            isInline = false,
            isExpect = false,
            returnType = builtins.unitType,
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

    // ------------------------------------ anonymous objects ------------------------------------

    fun createAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.Local,
        name: Name = SpecialNames.NO_NAME_PROVIDED,
        irParent: IrDeclarationParent? = null
    ): IrClass {
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        val irAnonymousObject = anonymousObject.convertWithOffsets { startOffset, endOffset ->
            IrFactoryImpl.createClass(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name,
                visibility = c.visibilityConverter.convertToDescriptorVisibility(visibility),
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

    // ------------------------------------ typealiases ------------------------------------

    fun createIrTypeAlias(
        typeAlias: FirTypeAlias,
        parent: IrDeclarationParent,
        symbol: IrTypeAliasSymbol,
    ): IrTypeAlias = typeAlias.convertWithOffsets { startOffset, endOffset ->
        classifierStorage.preCacheTypeParameters(typeAlias)
        IrFactoryImpl.createTypeAlias(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.DEFINED,
            name = typeAlias.name,
            visibility = c.visibilityConverter.convertToDescriptorVisibility(typeAlias.visibility),
            symbol = symbol,
            isActual = typeAlias.isActual,
            expandedType = typeAlias.expandedTypeRef.toIrType(c),
        ).apply {
            this.parent = parent
            this.metadata = FirMetadataSource.TypeAlias(typeAlias)
            setTypeParameters(this, typeAlias)
            setParent(parent)
            addDeclarationToParent(this, parent)
        }
    }

    // ------------------------------------ code fragments ------------------------------------

    fun createCodeFragmentClass(codeFragment: FirCodeFragment, containingFile: IrFile, symbol: IrClassSymbol): IrClass {
        val conversionData = extensions.codeFragmentConversionData(codeFragment)

        val irClass = codeFragment.convertWithOffsets { startOffset, endOffset ->
            IrFactoryImpl.createClass(
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
                isFun = false,
                hasEnumEntries = false,
            ).apply {
                metadata = FirMetadataSource.CodeFragment(codeFragment)
                setParent(containingFile)
                addDeclarationToParent(this, containingFile)
                typeParameters = emptyList()
                setThisReceiver(c, emptyList())
                superTypes = listOf(builtins.anyType)
            }
        }
        return irClass
    }

    // ------------------------------------ REPL snippets ------------------------------------

    @OptIn(LookupTagInternals::class)
    fun createEarlierSnippetClass(snippet: FirReplSnippet, containingPackageFragment: IrPackageFragment, symbol: IrClassSymbol): IrClass {
        val name = NameUtils.getScriptTargetClassName(snippet.name)
        val firSnippetClassSymbol = FirRegularClassSymbol(ClassId(containingPackageFragment.packageFqName, name))
        val firSnippetClass = buildRegularClass {
            moduleData = snippet.moduleData
            origin = FirDeclarationOrigin.FromOtherReplSnippet
            this.name = name
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            classKind = ClassKind.CLASS
            this.symbol = firSnippetClassSymbol
            superTypeRefs += session.builtinTypes.anyType
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            scopeProvider = session.kotlinScopeProvider
        }
        (firSnippetClass.symbol.toLookupTag() as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(session, firSnippetClassSymbol)
        return lazyDeclarationsGenerator.createIrLazyClass(firSnippetClass, containingPackageFragment, symbol)
    }

    // ------------------------------------ enum entries ------------------------------------

    fun createIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass,
        symbol: IrEnumEntrySymbol,
        predefinedOrigin: IrDeclarationOrigin? = null,
    ): IrEnumEntry {
        return enumEntry.convertWithOffsets { startOffset, endOffset ->
            val origin = enumEntry.computeIrOrigin(predefinedOrigin)
            IrFactoryImpl.createEnumEntry(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = enumEntry.name,
                symbol = symbol,
            ).apply {
                declarationStorage.enterScope(this.symbol)
                setParent(irParent)
                addDeclarationToParent(this, irParent)
                if (isEnumEntryWhichRequiresSubclass(enumEntry)) {
                    // An enum entry with its own members requires an anonymous object generated.
                    // Otherwise, this is a default-ish enum entry whose initializer would be a delegating constructor call,
                    // which will be translated via visitor later.
                    val klass = classifierStorage.getIrAnonymousObjectForEnumEntry(
                        (enumEntry.initializer as FirAnonymousObjectExpression).anonymousObject, enumEntry.name, irParent
                    )
                    this.correspondingClass = klass
                }
                if (origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
                    origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                ) {
                    // This [IrEnumEntry] won't be visited by [Fir2IrVisitor],
                    // hence annotation generation at this point.
                    annotationGenerator.generate(this, enumEntry)
                }
                declarationStorage.leaveScope(this.symbol)
            }
        }
    }

    private fun isEnumEntryWhichRequiresSubclass(enumEntry: FirEnumEntry): Boolean {
        val initializer = enumEntry.initializer
        return initializer is FirAnonymousObjectExpression && initializer.anonymousObject.declarations.any { it !is FirConstructor }
    }

    // ------------------------------------ utilities ------------------------------------

    internal fun setTypeParameters(
        irOwner: IrTypeParametersContainer,
        owner: FirTypeParameterRefsOwner,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ) {
        irOwner.typeParameters = owner.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index, typeOrigin).apply {
                parent = irOwner
                if (superTypes.isEmpty()) {
                    superTypes = typeParameter.bounds.map { it.toIrType(c, typeOrigin) }
                }
            }
        }
    }

    fun createIrClassForNotFoundClass(classLikeLookupTag: ConeClassLikeLookupTag): IrClass {
        val classId = classLikeLookupTag.classId
        val parentId = classId.outerClassId
        val parentClass = parentId?.let { classifierStorage.getIrClassForNotFoundClass(it.toLookupTag()) }
        val irParent = parentClass ?: declarationStorage.getIrExternalPackageFragment(
            classId.packageFqName, session.moduleData.dependencies.first()
        )

        return IrFactoryImpl.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = classId.shortClassName,
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            symbol = IrClassSymbolImpl(),
            kind = ClassKind.CLASS,
            modality = Modality.FINAL,
        ).apply {
            setParent(irParent)
            addDeclarationToParent(this, irParent)
            typeParameters = emptyList()
            setThisReceiver(c, emptyList())
            superTypes = listOf(builtins.anyType)
        }
    }
}
