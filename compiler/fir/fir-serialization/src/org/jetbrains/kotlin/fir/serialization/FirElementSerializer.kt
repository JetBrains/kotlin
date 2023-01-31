/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirCallableDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.projection
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.declarationForMetadataProviders
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.serialization.constant.EnumValue
import org.jetbrains.kotlin.fir.serialization.constant.IntValue
import org.jetbrains.kotlin.fir.serialization.constant.StringValue
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.deserialization.isKotlin1Dot4OrLater
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.RequireKotlinConstants
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class FirElementSerializer private constructor(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val containingDeclaration: FirDeclaration?,
    private val typeParameters: Interner<FirTypeParameter>,
    private val extension: FirSerializerExtension,
    private val typeTable: MutableTypeTable,
    private val versionRequirementTable: MutableVersionRequirementTable?,
    private val serializeTypeTableToFunction: Boolean,
    private val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    private val contractSerializer = FirContractSerializer()
    private val extensionDeclarationProviders = session.extensionService.declarationForMetadataProviders

    fun packagePartProto(packageFqName: FqName, files: List<FirFile>): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        fun addDeclaration(declaration: FirDeclaration, onUnsupportedDeclaration: (FirDeclaration) -> Unit) {
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirTypeAlias -> typeAliasProto(declaration)?.let { builder.addTypeAlias(it) }
                else -> onUnsupportedDeclaration(declaration)
            }
        }

        for (file in files) {
            for (declaration in file.declarations) {
                addDeclaration(declaration) {}
            }
        }

        extension.serializePackage(packageFqName, builder)
        for (extensionProvider in extensionDeclarationProviders) {
            for (declaration in extensionProvider.provideTopLevelDeclarations(packageFqName, scopeSession)) {
                addDeclaration(declaration) {
                    error("Unsupported top-level declaration type: ${it.render()}")
                }
            }
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable?.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    fun classProto(klass: FirClass): ProtoBuf.Class.Builder = whileAnalysing(session, klass) {
        val builder = ProtoBuf.Class.newBuilder()

        val regularClass = klass as? FirRegularClass
        val modality = regularClass?.modality ?: Modality.FINAL

        val hasEnumEntries = klass.classKind == ClassKind.ENUM_CLASS && languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)
        val flags = Flags.getClassFlags(
            klass.nonSourceAnnotations(session).isNotEmpty(),
            ProtoEnumFlags.visibility(regularClass?.let { normalizeVisibility(it) } ?: Visibilities.Local),
            ProtoEnumFlags.modality(modality),
            ProtoEnumFlags.classKind(klass.classKind, regularClass?.isCompanion == true),
            regularClass?.isInner == true,
            regularClass?.isData == true,
            regularClass?.isExternal == true,
            regularClass?.isExpect == true,
            regularClass?.isInline == true,
            regularClass?.isFun == true,
            hasEnumEntries,
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.fqName = getClassifierId(klass)

        for (typeParameter in klass.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            builder.addTypeParameter(typeParameterProto(typeParameter))
        }

        val classId = klass.symbol.classId
        if (classId != StandardClassIds.Any && classId != StandardClassIds.Nothing) {
            // Special classes (Any, Nothing) have no supertypes
            for (superTypeRef in klass.superTypeRefs) {
                if (useTypeTable()) {
                    builder.addSupertypeId(typeId(superTypeRef))
                } else {
                    builder.addSupertype(typeProto(superTypeRef))
                }
            }
        }

        val providedCallables = mutableListOf<FirCallableDeclaration>()
        val providedConstructors = mutableListOf<FirConstructor>()
        val providedNestedClassifiers = mutableListOf<FirClassifierSymbol<*>>()

        for (extensionProvider in extensionDeclarationProviders) {
            for (declaration in extensionProvider.provideDeclarationsForClass(klass, scopeSession)) {
                when (declaration) {
                    is FirConstructor -> providedConstructors += declaration
                    is FirCallableDeclaration -> providedCallables += declaration
                    is FirClassLikeDeclaration -> providedNestedClassifiers += declaration.symbol
                    else -> error("Unsupported declaration type in: ${klass.render()} ${declaration.render()}")
                }
            }
        }

        if (regularClass != null && regularClass.classKind != ClassKind.ENUM_ENTRY) {
            for (constructor in regularClass.constructors()) {
                builder.addConstructor(constructorProto(constructor))
            }
            for (constructor in providedConstructors) {
                builder.addConstructor(constructorProto(constructor))
            }
        }

        val callableMembers =
            extension.customClassMembersProducer?.getCallableMembers(klass)
                ?: (klass.memberDeclarations() + providedCallables)
                    .sortedWith(FirCallableDeclarationComparator)

        for (declaration in callableMembers) {
            if (declaration !is FirEnumEntry && declaration.isStatic) continue // ??? Miss values() & valueOf()
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirEnumEntry -> enumEntryProto(declaration).let { builder.addEnumEntry(it) }
                else -> {}
            }
        }

        fun FirClass.nestedClassifiers(): List<FirClassifierSymbol<*>> {
            val scope = defaultType().scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing, requiredPhase = null) ?: return emptyList()
            return buildList {
                scope.getClassifierNames().mapNotNullTo(this) { scope.getSingleClassifier(it) }
                addAll(providedNestedClassifiers)
            }
        }

        val nestedClassifiers = klass.nestedClassifiers()
        for (nestedClassifier in nestedClassifiers) {
            if (nestedClassifier is FirTypeAliasSymbol) {
                typeAliasProto(nestedClassifier.fir)?.let { builder.addTypeAlias(it) }
            } else if (nestedClassifier is FirRegularClassSymbol) {
                if (!extension.shouldSerializeNestedClass(nestedClassifier.fir)) {
                    continue
                }

                builder.addNestedClassName(getSimpleNameIndex(nestedClassifier.name))
            }
        }

        if (klass is FirRegularClass && klass.modality == Modality.SEALED) {
            val inheritors = klass.getSealedClassInheritors(session)
            for (inheritorId in inheritors) {
                builder.addSealedSubclassFqName(stringTable.getQualifiedClassNameIndex(inheritorId))
            }
        }

        val companionObject = regularClass?.companionObjectSymbol?.fir
        if (companionObject != null) {
            builder.companionObjectName = getSimpleNameIndex(companionObject.name)
        }

        val representation = (klass as? FirRegularClass)?.inlineClassRepresentation
        if (representation != null) {
            builder.inlineClassUnderlyingPropertyName = getSimpleNameIndex(representation.underlyingPropertyName)

            val property = callableMembers.single {
                it is FirProperty && it.receiverParameter == null && it.name == representation.underlyingPropertyName
            }

            if (!property.visibility.isPublicAPI) {
                if (useTypeTable()) {
                    builder.inlineClassUnderlyingTypeId = typeId(representation.underlyingType)
                } else {
                    builder.setInlineClassUnderlyingType(typeProto(representation.underlyingType))
                }
            }
        }

        if (klass is FirRegularClass) {
            for (contextReceiver in klass.contextReceivers) {
                val typeRef = contextReceiver.typeRef
                if (useTypeTable()) {
                    builder.addContextReceiverTypeId(typeId(typeRef))
                } else {
                    builder.addContextReceiverType(typeProto(contextReceiver.typeRef))
                }
            }
        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for classes: ${klass.render()}")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(klass))

        extension.serializeClass(klass, builder, versionRequirementTable, this)

        writeVersionRequirementForInlineClasses(klass, builder, versionRequirementTable)

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    private fun FirClass.memberDeclarations(): List<FirCallableDeclaration> {
        return collectDeclarations<FirCallableDeclaration, FirCallableSymbol<*>> { memberScope, addDeclarationIfNeeded ->
            memberScope.processAllFunctions { addDeclarationIfNeeded(it) }
            memberScope.processAllProperties { addDeclarationIfNeeded(it) }
        }
    }

    private fun FirClass.constructors(): List<FirConstructor> {
        return collectDeclarations { memberScope, addDeclarationIfNeeded ->
            memberScope.processDeclaredConstructors { addDeclarationIfNeeded(it) }
        }
    }

    private inline fun <reified T : FirCallableDeclaration, S : FirCallableSymbol<*>> FirClass.collectDeclarations(
        processScope: (FirTypeScope, ((S) -> Unit)) -> Unit
    ): List<T> = buildList {
        val memberScope = unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)

        processScope(memberScope) l@{
            val declaration = it.fir as T
            if (declaration.isSubstitutionOrIntersectionOverride) return@l

            // non-intersection or substitution fake override
            if (!(declaration.isStatic || declaration is FirConstructor)) {
                if (declaration.dispatchReceiverClassLookupTagOrNull()!= this@collectDeclarations.symbol.toLookupTag()) {
                    return@l
                }
            }

            add(declaration)
        }

        for (declaration in declarations) {
            if (declaration is T && declaration.isStatic) {
                add(declaration)
            }
        }
    }

    private fun FirPropertyAccessor.nonSourceAnnotations(session: FirSession): List<FirAnnotation> =
        (this as FirAnnotationContainer).nonSourceAnnotations(session)

    fun propertyProto(property: FirProperty): ProtoBuf.Property.Builder? = whileAnalysing(session, property) {
        if (!extension.shouldSerializeProperty(property)) return null

        val builder = ProtoBuf.Property.newBuilder()

        val local = createChildSerializer(property)

        var hasGetter = false
        var hasSetter = false

        val hasAnnotations = property.nonSourceAnnotations(session).isNotEmpty()
        // TODO: hasAnnotations(descriptor) || hasAnnotations(descriptor.backingField) || hasAnnotations(descriptor.delegateField)

        val modality = property.modality!!
        val defaultAccessorFlags = Flags.getAccessorFlags(
            hasAnnotations,
            ProtoEnumFlags.visibility(normalizeVisibility(property)),
            ProtoEnumFlags.modality(modality),
            false, false, false
        )

        val getter = property.getter
        if (getter != null) {
            hasGetter = true
            val accessorFlags = getAccessorFlags(getter, property)
            if (accessorFlags != defaultAccessorFlags) {
                builder.getterFlags = accessorFlags
            }
        }

        val setter = property.setter
        if (setter != null) {
            hasSetter = true
            val accessorFlags = getAccessorFlags(setter, property)
            if (accessorFlags != defaultAccessorFlags) {
                builder.setterFlags = accessorFlags
            }

            val nonSourceAnnotations = setter.nonSourceAnnotations(session)
            if (Flags.IS_NOT_DEFAULT.get(accessorFlags)) {
                val setterLocal = local.createChildSerializer(setter)
                for (valueParameterDescriptor in setter.valueParameters) {
                    val annotations = nonSourceAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER }
                    builder.setSetterValueParameter(setterLocal.valueParameterProto(valueParameterDescriptor, annotations))
                }
            }
        }

        val flags = Flags.getPropertyFlags(
            hasAnnotations,
            ProtoEnumFlags.visibility(normalizeVisibility(property)),
            ProtoEnumFlags.modality(modality),
            ProtoBuf.MemberKind.DECLARATION,
            property.isVar, hasGetter, hasSetter, property.isConst, property.isConst, property.isLateInit,
            property.isExternal, property.delegateFieldSymbol != null, property.isExpect
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(property.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(property.returnTypeRef)
        } else {
            builder.setReturnType(local.typeProto(property.returnTypeRef, toSuper = true))
        }

        for (typeParameter in property.typeParameters) {
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        for (contextReceiver in property.contextReceivers) {
            val typeRef = contextReceiver.typeRef
            if (useTypeTable()) {
                builder.addContextReceiverTypeId(typeId(typeRef))
            } else {
                builder.addContextReceiverType(typeProto(contextReceiver.typeRef))
            }
        }

        val receiverParameter = property.receiverParameter
        if (receiverParameter != null) {
            val receiverTypeRef = receiverParameter.typeRef
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverTypeRef)
            } else {
                builder.setReceiverType(local.typeProto(receiverTypeRef))
            }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(property))

            if (property.isSuspendOrHasSuspendTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirementDependingOnCoroutinesVersion())
            }

            if (property.hasInlineClassTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.InlineClasses))
            }
        }

        extension.serializeProperty(property, builder, versionRequirementTable, local)

        return builder
    }

    fun functionProto(function: FirFunction): ProtoBuf.Function.Builder? = whileAnalysing(session, function) {
        if (!extension.shouldSerializeFunction(function)) return null

        val builder = ProtoBuf.Function.newBuilder()
        val simpleFunction = function as? FirSimpleFunction

        val local = createChildSerializer(function)

        val flags = Flags.getFunctionFlags(
            function.nonSourceAnnotations(session).isNotEmpty(),
            ProtoEnumFlags.visibility(simpleFunction?.let { normalizeVisibility(it) } ?: Visibilities.Local),
            ProtoEnumFlags.modality(simpleFunction?.modality ?: Modality.FINAL),
            ProtoBuf.MemberKind.DECLARATION,
            simpleFunction?.isOperator == true,
            simpleFunction?.isInfix == true,
            simpleFunction?.isInline == true,
            simpleFunction?.isTailRec == true,
            simpleFunction?.isExternal == true,
            simpleFunction?.isSuspend == true,
            simpleFunction?.isExpect == true,
            true // TODO: supply 'hasStableParameterNames' flag for metadata
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        val name = when (function) {
            is FirSimpleFunction -> {
                function.name
            }
            is FirAnonymousFunction -> {
                if (function.isLambda) SpecialNames.ANONYMOUS else Name.special("<no name provided>")
            }
            else -> throw AssertionError("Unsupported function: ${function.render()}")
        }
        builder.name = getSimpleNameIndex(name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(function.returnTypeRef)
        } else {
            builder.setReturnType(local.typeProto(function.returnTypeRef, toSuper = true))
        }

        for (typeParameter in function.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        for (contextReceiver in function.contextReceivers) {
            val typeRef = contextReceiver.typeRef
            if (useTypeTable()) {
                builder.addContextReceiverTypeId(typeId(typeRef))
            } else {
                builder.addContextReceiverType(typeProto(contextReceiver.typeRef))
            }
        }

        val receiverParameter = function.receiverParameter
        if (receiverParameter != null) {
            val receiverTypeRef = receiverParameter.typeRef
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverTypeRef)
            } else {
                builder.setReceiverType(local.typeProto(receiverTypeRef))
            }
        }

        for (valueParameter in function.valueParameters) {
            builder.addValueParameter(local.valueParameterProto(valueParameter))
        }

        contractSerializer.serializeContractOfFunctionIfAny(function, builder, this)

        extension.serializeFunction(function, builder, versionRequirementTable, local)

        if (serializeTypeTableToFunction) {
            typeTable.serialize()?.let { builder.typeTable = it }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(function))

            if (function.isSuspendOrHasSuspendTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirementDependingOnCoroutinesVersion())
            }

            if (function.hasInlineClassTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.InlineClasses))
            }
        }

        return builder
    }

    private fun typeAliasProto(typeAlias: FirTypeAlias): ProtoBuf.TypeAlias.Builder? = whileAnalysing(session, typeAlias) {
        if (!extension.shouldSerializeTypeAlias(typeAlias)) return null

        val builder = ProtoBuf.TypeAlias.newBuilder()
        val local = createChildSerializer(typeAlias)

        val flags = Flags.getTypeAliasFlags(
            typeAlias.nonSourceAnnotations(session).isNotEmpty(),
            ProtoEnumFlags.visibility(normalizeVisibility(typeAlias))
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(typeAlias.name)

        for (typeParameter in typeAlias.typeParameters) {
            builder.addTypeParameter(local.typeParameterProto(typeParameter))
        }

        val underlyingType = typeAlias.expandedConeType!!
        if (useTypeTable()) {
            builder.underlyingTypeId = local.typeId(underlyingType)
        } else {
            builder.setUnderlyingType(local.typeProto(underlyingType))
        }

        val expandedType = underlyingType.fullyExpandedType(session)
        if (useTypeTable()) {
            builder.expandedTypeId = local.typeId(expandedType)
        } else {
            builder.setExpandedType(local.typeProto(expandedType))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(typeAlias))
        }

        for (annotation in typeAlias.nonSourceAnnotations(session)) {
            builder.addAnnotation(extension.annotationSerializer.serializeAnnotation(annotation))
        }

        extension.serializeTypeAlias(typeAlias, builder)

        return builder
    }

    private fun enumEntryProto(enumEntry: FirEnumEntry): ProtoBuf.EnumEntry.Builder = whileAnalysing(session, enumEntry) {
        val builder = ProtoBuf.EnumEntry.newBuilder()
        builder.name = getSimpleNameIndex(enumEntry.name)
        extension.serializeEnumEntry(enumEntry, builder)
        return builder
    }

    private fun constructorProto(constructor: FirConstructor): ProtoBuf.Constructor.Builder = whileAnalysing(session, constructor) {
        val builder = ProtoBuf.Constructor.newBuilder()

        val local = createChildSerializer(constructor)

        val flags = Flags.getConstructorFlags(
            constructor.nonSourceAnnotations(session).isNotEmpty(),
            ProtoEnumFlags.visibility(normalizeVisibility(constructor)),
            !constructor.isPrimary,
            true // TODO: supply 'hasStableParameterNames' flag for metadata
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        for (valueParameter in constructor.valueParameters) {
            builder.addValueParameter(local.valueParameterProto(valueParameter))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(constructor))

            if (constructor.isSuspendOrHasSuspendTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirementDependingOnCoroutinesVersion())
            }

            if (constructor.hasInlineClassTypesInSignature()) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.InlineClasses))
            }
        }

        extension.serializeConstructor(constructor, builder, local)

        return builder
    }

    private fun valueParameterProto(
        parameter: FirValueParameter,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): ProtoBuf.ValueParameter.Builder = whileAnalysing(session, parameter) {
        val builder = ProtoBuf.ValueParameter.newBuilder()

        val declaresDefaultValue = parameter.defaultValue != null // TODO: || parameter.isActualParameterWithAnyExpectedDefault

        val flags = Flags.getValueParameterFlags(
            additionalAnnotations.isNotEmpty() || parameter.nonSourceAnnotations(session).isNotEmpty(),
            declaresDefaultValue,
            parameter.isCrossinline,
            parameter.isNoinline
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(parameter.name)

        if (useTypeTable()) {
            builder.typeId = typeId(parameter.returnTypeRef)
        } else {
            builder.setType(typeProto(parameter.returnTypeRef))
        }

        if (parameter.isVararg) {
            val varargElementType = parameter.returnTypeRef.coneType.varargElementType()
            if (useTypeTable()) {
                builder.varargElementTypeId = typeId(varargElementType)
            } else {
                builder.setVarargElementType(typeProto(varargElementType))
            }
        }

        extension.serializeValueParameter(parameter, builder)

        return builder
    }

    private fun typeParameterProto(typeParameter: FirTypeParameter): ProtoBuf.TypeParameter.Builder = whileAnalysing(session, typeParameter) {
        val builder = ProtoBuf.TypeParameter.newBuilder()

        builder.id = getTypeParameterId(typeParameter)

        builder.name = getSimpleNameIndex(typeParameter.name)

        if (typeParameter.isReified != builder.reified) {
            builder.reified = typeParameter.isReified
        }

        val variance = ProtoEnumFlags.variance(typeParameter.variance)
        if (variance != builder.variance) {
            builder.variance = variance
        }
        extension.serializeTypeParameter(typeParameter, builder)

        val upperBounds = typeParameter.bounds
        if (upperBounds.size == 1 && upperBounds.single() is FirImplicitNullableAnyTypeRef) return builder

        for (upperBound in upperBounds) {
            if (useTypeTable()) {
                builder.addUpperBoundId(typeId(upperBound))
            } else {
                builder.addUpperBound(typeProto(upperBound))
            }
        }

        return builder
    }

    fun typeId(typeRef: FirTypeRef): Int {
        if (typeRef !is FirResolvedTypeRef) {
            return -1 // TODO: serializeErrorType?
        }
        return typeId(typeRef.type)
    }

    fun typeId(type: ConeKotlinType): Int = typeTable[typeProto(type)]

    private fun typeProto(typeRef: FirTypeRef, toSuper: Boolean = false): ProtoBuf.Type.Builder {
        return typeProto(typeRef.coneType, toSuper, correspondingTypeRef = typeRef)
    }

    private fun typeProto(
        type: ConeKotlinType,
        toSuper: Boolean = false,
        correspondingTypeRef: FirTypeRef? = null,
        isDefinitelyNotNullType: Boolean = false,
    ): ProtoBuf.Type.Builder {
        val typeProto = typeOrTypealiasProto(type, toSuper, correspondingTypeRef, isDefinitelyNotNullType)
        val expanded = if (type is ConeClassLikeType) type.fullyExpandedType(session) else type
        if (expanded === type) {
            return typeProto
        }
        val expandedProto = typeOrTypealiasProto(expanded, toSuper, correspondingTypeRef, isDefinitelyNotNullType)
        if (useTypeTable()) {
            expandedProto.abbreviatedTypeId = typeTable[typeProto]
        } else {
            expandedProto.setAbbreviatedType(typeProto)
        }
        return expandedProto
    }

    private fun typeOrTypealiasProto(
        type: ConeKotlinType,
        toSuper: Boolean,
        correspondingTypeRef: FirTypeRef?,
        isDefinitelyNotNullType: Boolean,
    ): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()
        when (type) {
            is ConeDefinitelyNotNullType -> return typeProto(type.original, toSuper, correspondingTypeRef, isDefinitelyNotNullType = true)
            is ConeErrorType -> {
                extension.serializeErrorType(type, builder)
                return builder
            }
            is ConeFlexibleType -> {
                val lowerBound = typeProto(type.lowerBound)
                val upperBound = typeProto(type.upperBound)
                extension.serializeFlexibleType(type, lowerBound, upperBound)
                if (useTypeTable()) {
                    lowerBound.flexibleUpperBoundId = typeTable[upperBound]
                } else {
                    lowerBound.setFlexibleUpperBound(upperBound)
                }
                return lowerBound
            }
            is ConeClassLikeType -> {
                if (type.functionTypeKind(session) == FunctionTypeKind.SuspendFunction) {
                    val runtimeFunctionType = type.suspendFunctionTypeToFunctionTypeWithContinuation(
                        session, StandardClassIds.Continuation
                    )
                    val functionType = typeProto(runtimeFunctionType)
                    functionType.flags = Flags.getTypeFlags(true, false)
                    return functionType
                }
                fillFromPossiblyInnerType(builder, type)
                if (type.hasContextReceivers) {
                    serializeAnnotationFromAttribute(
                        correspondingTypeRef?.annotations, CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID, builder,
                        argumentMapping = buildAnnotationArgumentMapping {
                            this.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] =
                                buildConstExpression(source = null, ConstantValueKind.Int, type.contextReceiversNumberForFunctionType)
                        }
                    )
                }
            }
            is ConeTypeParameterType -> {
                val typeParameter = type.lookupTag.typeParameterSymbol.fir
                if (typeParameter in ((containingDeclaration as? FirMemberDeclaration)?.typeParameters ?: emptyList())) {
                    builder.typeParameterName = getSimpleNameIndex(typeParameter.name)
                } else {
                    builder.typeParameter = getTypeParameterId(typeParameter)
                }

                if (isDefinitelyNotNullType) {
                    builder.flags = Flags.getTypeFlags(false, isDefinitelyNotNullType)
                }
            }
            is ConeIntersectionType -> {
                val approximatedType = if (toSuper) {
                    typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes)
                } else {
                    typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes)
                }
                assert(approximatedType != type && approximatedType is ConeKotlinType) {
                    "Approximation failed: ${type.renderForDebugging()}"
                }
                return typeProto(approximatedType as ConeKotlinType)
            }
            is ConeIntegerLiteralType -> {
                throw IllegalStateException("Integer literal types should not persist up to the serializer: ${type.renderForDebugging()}")
            }
            is ConeCapturedType -> {
                throw IllegalStateException("Captured types should not persist up to the serializer: ${type.renderForDebugging()}")
            }
            else -> {
                throw AssertionError("Should not be here: ${type::class.java}")
            }
        }

        if (type.isMarkedNullable != builder.nullable) {
            builder.nullable = type.isMarkedNullable
        }

        val extensionAttributes = mutableListOf<ConeAttribute<*>>()
        for (attribute in type.attributes) {
            when {
                attribute is CustomAnnotationTypeAttribute ->
                    for (annotation in attribute.annotations.nonSourceAnnotations(session)) {
                        extension.serializeTypeAnnotation(annotation, builder)
                    }
                attribute.key in CompilerConeAttributes.classIdByCompilerAttributeKey ->
                    serializeCompilerDefinedTypeAttribute(builder, attribute)
                else -> extensionAttributes += attribute
            }
        }

        for (attributeExtension in session.extensionService.typeAttributeExtensions) {
            for (attribute in extensionAttributes) {
                val annotation = attributeExtension.convertAttributeToAnnotation(attribute) ?: continue
                extension.serializeTypeAnnotation(annotation, builder)
            }
        }

        // TODO: abbreviated type
//        val abbreviation = type.getAbbreviatedType()?.abbreviation
//        if (abbreviation != null) {
//            if (useTypeTable()) {
//                builder.abbreviatedTypeId = typeId(abbreviation)
//            } else {
//                builder.setAbbreviatedType(typeProto(abbreviation))
//            }
//        }

        return builder
    }

    private fun serializeCompilerDefinedTypeAttribute(
        builder: ProtoBuf.Type.Builder,
        attribute: ConeAttribute<*>
    ) {
        val annotation = buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                this.type = ConeClassLikeTypeImpl(
                    CompilerConeAttributes.classIdByCompilerAttributeKey.getValue(attribute.key).toLookupTag(),
                    emptyArray(),
                    isNullable = false
                )
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
        extension.serializeTypeAnnotation(annotation, builder)
    }

    private fun serializeAnnotationFromAttribute(
        existingAnnotations: List<FirAnnotation>?,
        classId: ClassId,
        builder: ProtoBuf.Type.Builder,
        argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping,
    ) {
        if (existingAnnotations?.any { it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId == classId } != true) {
            extension.serializeTypeAnnotation(
                buildAnnotation {
                    annotationTypeRef = buildResolvedTypeRef {
                        this.type = classId.constructClassLikeType(
                            emptyArray(), isNullable = false
                        )
                    }
                    this.argumentMapping = argumentMapping
                }, builder
            )
        }
    }

    private fun fillFromPossiblyInnerType(builder: ProtoBuf.Type.Builder, type: ConeClassLikeType) {
        val classifierSymbol = type.lookupTag.toSymbol(session)
        if (classifierSymbol != null) {
            val classifier = classifierSymbol.fir
            val classifierId = getClassifierId(classifier)
            if (classifier is FirTypeAlias) {
                builder.typeAliasName = classifierId
            } else {
                builder.className = classifierId
            }
        } else {
            builder.className = getClassifierId(type.lookupTag.classId)
        }

        for (projection in type.typeArguments) {
            builder.addArgument(typeArgument(projection))
        }

        // TODO: outer type
//        if (type.outerType != null) {
//            val outerBuilder = ProtoBuf.Type.newBuilder()
//            fillFromPossiblyInnerType(outerBuilder, type.outerType!!)
//            if (useTypeTable()) {
//                builder.outerTypeId = typeTable[outerBuilder]
//            } else {
//                builder.setOuterType(outerBuilder)
//            }
//        }
    }

    private fun typeArgument(typeProjection: ConeTypeProjection): ProtoBuf.Type.Argument.Builder {
        val builder = ProtoBuf.Type.Argument.newBuilder()

        if (typeProjection is ConeStarProjection) {
            builder.projection = ProtoBuf.Type.Argument.Projection.STAR
        } else if (typeProjection is ConeKotlinTypeProjection) {
            val projection = ProtoEnumFlags.projection(typeProjection.kind)

            if (projection != builder.projection) {
                builder.projection = projection
            }

            if (useTypeTable()) {
                builder.typeId = typeId(typeProjection.type)
            } else {
                builder.setType(typeProto(typeProjection.type))
            }
        }

        return builder
    }

    private fun getAccessorFlags(accessor: FirPropertyAccessor, property: FirProperty): Int {
        // [FirDefaultPropertyAccessor]---a property accessor without body---can still hold other information, such as annotations,
        // user-contributed visibility, and modifiers, such as `external` or `inline`.
        val nonSourceAnnotations = accessor.nonSourceAnnotations(session)
        val isDefault = accessor is FirDefaultPropertyAccessor &&
                nonSourceAnnotations.isEmpty() &&
                accessor.visibility == property.visibility &&
                !accessor.isExternal &&
                !accessor.isInline
        return Flags.getAccessorFlags(
            nonSourceAnnotations.isNotEmpty(),
            ProtoEnumFlags.visibility(normalizeVisibility(accessor)),
            ProtoEnumFlags.modality(accessor.modality!!),
            !isDefault,
            accessor.isExternal,
            accessor.isInline
        )
    }

    private fun createChildSerializer(declaration: FirDeclaration): FirElementSerializer =
        FirElementSerializer(
            session, scopeSession, declaration, Interner(typeParameters), extension,
            typeTable, versionRequirementTable, serializeTypeTableToFunction = false,
            typeApproximator, languageVersionSettings
        )

    val stringTable: FirElementAwareStringTable
        get() = extension.stringTable

    private fun useTypeTable(): Boolean = extension.shouldUseTypeTable()

    private fun FirDeclaration.hasInlineClassTypesInSignature(): Boolean {
        // TODO
        return false
    }

    private fun FirCallableDeclaration.isSuspendOrHasSuspendTypesInSignature(): Boolean {
        // TODO (types in signature)
        return this.isSuspend
    }

    private fun writeVersionRequirementForInlineClasses(
        klass: FirClass,
        builder: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable
    ) {
        if (klass !is FirRegularClass || !klass.isInline && !klass.hasInlineClassTypesInSignature()) return

        builder.addVersionRequirement(
            writeLanguageVersionRequirement(LanguageFeature.InlineClasses, versionRequirementTable)
        )
    }

    private fun MutableVersionRequirementTable.serializeVersionRequirements(container: FirAnnotationContainer): List<Int> =
        serializeVersionRequirements(container.annotations)

    private fun MutableVersionRequirementTable.serializeVersionRequirements(annotations: List<FirAnnotation>): List<Int> =
        annotations
            .filter {
                it.toAnnotationClassId(session)?.asSingleFqName() == RequireKotlinConstants.FQ_NAME
            }
            .mapNotNull(::serializeVersionRequirementFromRequireKotlin)
            .map(::get)

    private fun MutableVersionRequirementTable.writeVersionRequirement(languageFeature: LanguageFeature): Int {
        return writeLanguageVersionRequirement(languageFeature, this)
    }

    private fun MutableVersionRequirementTable.writeVersionRequirementDependingOnCoroutinesVersion(): Int =
        writeVersionRequirement(LanguageFeature.ReleaseCoroutines)

    private fun serializeVersionRequirementFromRequireKotlin(annotation: FirAnnotation): ProtoBuf.VersionRequirement.Builder? {
        val argumentMapping = annotation.argumentMapping.mapping

        val versionString = (argumentMapping[RequireKotlinConstants.VERSION]?.toConstantValue(session) as? StringValue)?.value ?: return null
        val matchResult = RequireKotlinConstants.VERSION_REGEX.matchEntire(versionString) ?: return null

        val major = matchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = matchResult.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = matchResult.groupValues.getOrNull(4)?.toIntOrNull() ?: 0

        val proto = ProtoBuf.VersionRequirement.newBuilder()
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { proto.version = it },
            writeVersionFull = { proto.versionFull = it }
        )

        val message = (argumentMapping[RequireKotlinConstants.MESSAGE]?.toConstantValue(session) as? StringValue)?.value
        if (message != null) {
            proto.message = stringTable.getStringIndex(message)
        }

        when ((argumentMapping[RequireKotlinConstants.LEVEL]?.toConstantValue(session) as? EnumValue)?.enumEntryName?.asString()) {
            DeprecationLevel.ERROR.name -> {
                // ERROR is the default level
            }
            DeprecationLevel.WARNING.name -> proto.level = ProtoBuf.VersionRequirement.Level.WARNING
            DeprecationLevel.HIDDEN.name -> proto.level = ProtoBuf.VersionRequirement.Level.HIDDEN
        }

        when ((argumentMapping[RequireKotlinConstants.VERSION_KIND]?.toConstantValue(session) as? EnumValue)?.enumEntryName?.asString()) {
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION.name -> {
                // LANGUAGE_VERSION is the default kind
            }
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            ProtoBuf.VersionRequirement.VersionKind.API_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.API_VERSION
        }

        val errorCode = (argumentMapping[RequireKotlinConstants.ERROR_CODE]?.toConstantValue(session) as? IntValue)?.value
        if (errorCode != null && errorCode != -1) {
            proto.errorCode = errorCode
        }

        return proto
    }

    private operator fun FirArgumentList.get(name: Name): FirExpression? {
        // TODO: constant evaluation
        return arguments.filterIsInstance<FirNamedArgumentExpression>().find {
            it.name == name
        }?.expression
    }


    private fun normalizeVisibility(declaration: FirMemberDeclaration): Visibility {
        return declaration.visibility.normalize()
    }

    private fun normalizeVisibility(declaration: FirPropertyAccessor): Visibility {
        return declaration.visibility.normalize()
    }

    private fun getClassifierId(declaration: FirClassLikeDeclaration): Int =
        stringTable.getFqNameIndex(declaration)

    private fun getClassifierId(classId: ClassId): Int =
        stringTable.getQualifiedClassNameIndex(classId)

    private fun getSimpleNameIndex(name: Name): Int =
        stringTable.getStringIndex(name.asString())

    private fun getTypeParameterId(typeParameter: FirTypeParameter): Int =
        typeParameters.intern(typeParameter)

    companion object {
        @JvmStatic
        fun createTopLevel(
            session: FirSession,
            scopeSession: ScopeSession,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
            )

        @JvmStatic
        fun createForLambda(
            session: FirSession,
            scopeSession: ScopeSession,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(),
                versionRequirementTable = null, serializeTypeTableToFunction = true,
                typeApproximator,
                languageVersionSettings,
            )

        @JvmStatic
        fun create(
            session: FirSession,
            scopeSession: ScopeSession,
            klass: FirClass,
            extension: FirSerializerExtension,
            parentSerializer: FirElementSerializer?,
            typeApproximator: AbstractTypeApproximator,
            languageVersionSettings: LanguageVersionSettings,
        ): FirElementSerializer {
            val parentClassId = klass.symbol.classId.outerClassId
            val parent = if (parentClassId != null && !parentClassId.isLocal) {
                val parentClass = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)!!.fir as FirRegularClass
                parentSerializer ?: create(
                    session, scopeSession, parentClass, extension, null, typeApproximator,
                    languageVersionSettings,
                )
            } else {
                createTopLevel(session, scopeSession, extension, typeApproximator, languageVersionSettings)
            }

            // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
            // serializing outer classes before nested classes.
            // Otherwise our interner can get wrong ids because we may serialize classes in any order.
            val serializer = FirElementSerializer(
                session,
                scopeSession,
                klass,
                Interner(parent.typeParameters),
                extension,
                MutableTypeTable(),
                if (parentClassId != null && !isKotlin1Dot4OrLater(extension.metadataVersion)) {
                    parent.versionRequirementTable
                } else {
                    MutableVersionRequirementTable()
                },
                serializeTypeTableToFunction = false,
                typeApproximator,
                languageVersionSettings,
            )
            for (typeParameter in klass.typeParameters) {
                if (typeParameter !is FirTypeParameter) continue
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }

        fun writeLanguageVersionRequirement(
            languageFeature: LanguageFeature,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val languageVersion = languageFeature.sinceVersion!!
            return writeVersionRequirement(
                languageVersion.major, languageVersion.minor, 0,
                ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION,
                versionRequirementTable
            )
        }

        fun writeVersionRequirement(
            major: Int,
            minor: Int,
            patch: Int,
            versionKind: ProtoBuf.VersionRequirement.VersionKind,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val requirement = ProtoBuf.VersionRequirement.newBuilder().apply {
                VersionRequirement.Version(major, minor, patch).encode(
                    writeVersion = { version = it },
                    writeVersionFull = { versionFull = it }
                )
                if (versionKind != defaultInstanceForType.versionKind) {
                    this.versionKind = versionKind
                }
            }
            return versionRequirementTable[requirement]
        }
    }
}
