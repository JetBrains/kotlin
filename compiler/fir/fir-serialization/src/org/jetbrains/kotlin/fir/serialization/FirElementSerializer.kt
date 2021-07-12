/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.config.LanguageFeature
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
import org.jetbrains.kotlin.fir.deserialization.CONTINUATION_INTERFACE_CLASS_ID
import org.jetbrains.kotlin.fir.deserialization.projection
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.references.impl.FirReferencePlaceholderForResolvedAnnotations
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.varargElementType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.suspendFunctionTypeToFunctionTypeWithContinuation
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.serialization.constant.EnumValue
import org.jetbrains.kotlin.fir.serialization.constant.IntValue
import org.jetbrains.kotlin.fir.serialization.constant.StringValue
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.deserialization.isKotlin1Dot4OrLater
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.RequireKotlinConstants
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.types.AbstractTypeApproximator
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
) {
    private val contractSerializer = FirContractSerializer()

    fun packagePartProto(packageFqName: FqName, file: FirFile): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        for (declaration in file.declarations) {
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirTypeAlias -> typeAliasProto(declaration)?.let { builder.addTypeAlias(it) }
            }
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        val versionRequirementTableProto = versionRequirementTable?.serialize()
        if (versionRequirementTableProto != null) {
            builder.versionRequirementTable = versionRequirementTableProto
        }

        extension.serializePackage(packageFqName, builder)

        return builder
    }

    fun classProto(klass: FirClass): ProtoBuf.Class.Builder {
        val builder = ProtoBuf.Class.newBuilder()

        val regularClass = klass as? FirRegularClass
        val modality = regularClass?.modality ?: Modality.FINAL
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
            regularClass?.isFun == true
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

        if (regularClass != null && regularClass.classKind != ClassKind.ENUM_ENTRY) {
            for (constructor in regularClass.declarations.filterIsInstance<FirConstructor>()) {
                builder.addConstructor(constructorProto(constructor))
            }
        }

        val callableMembers =
            extension.customClassMembersProducer?.getCallableMembers(klass)
                ?: klass.declarations()
                    .sortedWith(FirCallableDeclarationComparator)

        for (declaration in callableMembers) {
            if (declaration !is FirEnumEntry && declaration.isStatic) continue // ??? Miss values() & valueOf()
            when (declaration) {
                is FirProperty -> propertyProto(declaration)?.let { builder.addProperty(it) }
                is FirSimpleFunction -> functionProto(declaration)?.let { builder.addFunction(it) }
                is FirEnumEntry -> enumEntryProto(declaration).let { builder.addEnumEntry(it) }
            }
        }

        val nestedClassifiers = klass.declarations.filterIsInstance<FirClassLikeDeclaration>()
        for (nestedClassifier in nestedClassifiers) {
            if (nestedClassifier is FirTypeAlias) {
                typeAliasProto(nestedClassifier)?.let { builder.addTypeAlias(it) }
            } else if (nestedClassifier is FirRegularClass) {
                if (!extension.shouldSerializeNestedClass(nestedClassifier)) {
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

        val companionObject = regularClass?.companionObject
        if (companionObject != null) {
            builder.companionObjectName = getSimpleNameIndex(companionObject.name)
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        val representation = (klass as? FirRegularClass)?.getInlineClassUnderlyingParameter()
        if (representation != null) {
            builder.inlineClassUnderlyingPropertyName = getSimpleNameIndex(representation.name)

            val property = callableMembers.single {
                it is FirProperty && it.receiverTypeRef == null && it.name == representation.name
            }
            if (!property.visibility.isPublicAPI) {
                if (useTypeTable()) {
                    builder.inlineClassUnderlyingTypeId = typeId(representation.returnTypeRef)
                } else {
                    builder.setInlineClassUnderlyingType(typeProto(representation.returnTypeRef))
                }
            }
        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for classes: ${klass.render()}")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(klass))

        extension.serializeClass(klass, builder, versionRequirementTable, this)

        writeVersionRequirementForInlineClasses(klass, builder, versionRequirementTable)

        val versionRequirementTableProto = versionRequirementTable.serialize()
        if (versionRequirementTableProto != null) {
            builder.versionRequirementTable = versionRequirementTableProto
        }
        return builder
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirClass.declarations(): List<FirCallableDeclaration> = buildList {
        val memberScope =
            defaultType().scope(session, scopeSession, FakeOverrideTypeCalculator.DoNothing)
                ?: error("Null scope for $this")

        fun addDeclarationIfNeeded(symbol: FirCallableSymbol<*>) {
            val declaration = symbol.fir
            if (declaration.isSubstitutionOrIntersectionOverride) return

            // non-intersection or substitution fake override
            if (!declaration.isStatic && declaration.dispatchReceiverClassOrNull() != this@declarations.symbol.toLookupTag()) return

            add(declaration)
        }

        memberScope.processAllFunctions(::addDeclarationIfNeeded)
        memberScope.processAllProperties(::addDeclarationIfNeeded)

        for (declaration in declarations) {
            if (declaration is FirCallableDeclaration && declaration.isStatic) {
                add(declaration)
            }
        }
    }

    private fun FirPropertyAccessor.nonSourceAnnotations(session: FirSession, property: FirProperty): List<FirAnnotationCall> =
        (this as FirAnnotationContainer).nonSourceAnnotations(session) + property.nonSourceAnnotations(session).filter {
            val useSiteTarget = it.useSiteTarget
            useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER && isGetter ||
                    useSiteTarget == AnnotationUseSiteTarget.PROPERTY_SETTER && isSetter ||
                    useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER && isSetter
        }

    fun propertyProto(property: FirProperty): ProtoBuf.Property.Builder? {
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

            val nonSourceAnnotations = setter.nonSourceAnnotations(session, property)
            if (setter !is FirDefaultPropertyAccessor ||
                nonSourceAnnotations.isNotEmpty() ||
                setter.visibility != property.visibility
            ) {
                val setterLocal = local.createChildSerializer(setter)
                for (valueParameterDescriptor in setter.valueParameters) {
                    val annotations = nonSourceAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER }
                    builder.setSetterValueParameter(setterLocal.valueParameterProto(valueParameterDescriptor, annotations))
                }
            }
        }

        val hasConstant = property.isConst // TODO: this is only correct with LanguageFeature.NoConstantValueAttributeForNonConstVals
        val flags = Flags.getPropertyFlags(
            hasAnnotations,
            ProtoEnumFlags.visibility(normalizeVisibility(property)),
            ProtoEnumFlags.modality(modality),
            ProtoBuf.MemberKind.DECLARATION,
            property.isVar, hasGetter, hasSetter, hasConstant, property.isConst, property.isLateInit,
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

        val receiverTypeRef = property.receiverTypeRef
        if (receiverTypeRef != null) {
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

    fun functionProto(function: FirFunction): ProtoBuf.Function.Builder? {
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
                if (function.isLambda) Name.special("<anonymous>") else Name.special("<no name provided>")
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

        val receiverTypeRef = function.receiverTypeRef
        if (receiverTypeRef != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverTypeRef)
            } else {
                builder.setReceiverType(local.typeProto(receiverTypeRef))
            }
        }

        for (valueParameter in function.valueParameters) {
            builder.addValueParameter(local.valueParameterProto(valueParameter))
        }

        if (serializeTypeTableToFunction) {
            val typeTableProto = typeTable.serialize()
            if (typeTableProto != null) {
                builder.typeTable = typeTableProto
            }
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

        contractSerializer.serializeContractOfFunctionIfAny(function, builder, this)

        extension.serializeFunction(function, builder, versionRequirementTable, local)

        return builder
    }

    private fun typeAliasProto(typeAlias: FirTypeAlias): ProtoBuf.TypeAlias.Builder? {
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

    private fun enumEntryProto(enumEntry: FirEnumEntry): ProtoBuf.EnumEntry.Builder {
        val builder = ProtoBuf.EnumEntry.newBuilder()
        builder.name = getSimpleNameIndex(enumEntry.name)
        extension.serializeEnumEntry(enumEntry, builder)
        return builder
    }

    private fun constructorProto(constructor: FirConstructor): ProtoBuf.Constructor.Builder {
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
        additionalAnnotations: List<FirAnnotationCall> = emptyList()
    ): ProtoBuf.ValueParameter.Builder {
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

    private fun typeParameterProto(typeParameter: FirTypeParameter): ProtoBuf.TypeParameter.Builder {
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
        return typeProto(typeRef.coneType, toSuper, correspondingTypeRef = typeRef).also {
            for (annotation in typeRef.annotations) {
                extension.serializeTypeAnnotation(annotation, it)
            }
        }
    }

    private fun typeProto(
        type: ConeKotlinType,
        toSuper: Boolean = false,
        correspondingTypeRef: FirTypeRef? = null,
        isDefinitelyNotNullType: Boolean = false,
    ): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()

        when (type) {
            is ConeDefinitelyNotNullType -> return typeProto(type.original, toSuper, correspondingTypeRef, isDefinitelyNotNullType = true)
            is ConeKotlinErrorType -> {
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
                if (type.isSuspendFunctionType(session)) {
                    val runtimeFunctionType = type.suspendFunctionTypeToFunctionTypeWithContinuation(
                        session, CONTINUATION_INTERFACE_CLASS_ID
                    )
                    val functionType = typeProto(runtimeFunctionType)
                    functionType.flags = Flags.getTypeFlags(true, false)
                    return functionType
                }
                fillFromPossiblyInnerType(builder, type)
                if (type.isExtensionFunctionType) {
                    serializeAnnotationFromAttribute(
                        correspondingTypeRef?.annotations, CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID, builder
                    )
                }
            }
            is ConeTypeParameterType -> {
                val typeParameter = type.lookupTag.typeParameterSymbol.fir
                if (typeParameter in (containingDeclaration as? FirMemberDeclaration)?.typeParameters ?: emptyList()) {
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
                    typeApproximator.approximateToSuperType(type, TypeApproximatorConfiguration.PublicDeclaration)
                } else {
                    typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.PublicDeclaration)
                }
                assert(approximatedType != type && approximatedType is ConeKotlinType) {
                    "Approximation failed: ${type.render()}"
                }
                return typeProto(approximatedType as ConeKotlinType)
            }
            is ConeIntegerLiteralType -> {
                throw IllegalStateException("Integer literal types should not persist up to the serializer: ${type.render()}")
            }
            is ConeCapturedType -> {
                throw IllegalStateException("Captured types should not persist up to the serializer: ${type.render()}")
            }
            else -> {
                throw AssertionError("Should not be here: ${type::class.java}")
            }
        }

        if (type.isMarkedNullable != builder.nullable) {
            builder.nullable = type.isMarkedNullable
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

    private fun serializeAnnotationFromAttribute(
        existingAnnotations: List<FirAnnotationCall>?,
        classId: ClassId,
        builder: ProtoBuf.Type.Builder
    ) {
        if (existingAnnotations?.any { it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId == classId } != true) {
            extension.serializeTypeAnnotation(
                buildAnnotationCall {
                    calleeReference = FirReferencePlaceholderForResolvedAnnotations
                    annotationTypeRef = buildResolvedTypeRef {
                        this.type = CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID.constructClassLikeType(
                            emptyArray(), isNullable = false
                        )
                    }
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
        val nonSourceAnnotations = accessor.nonSourceAnnotations(session, property)
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
            typeApproximator
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

    private fun MutableVersionRequirementTable.serializeVersionRequirements(annotations: List<FirAnnotationCall>): List<Int> =
        annotations
            .filter {
                it.toAnnotationClassId().asSingleFqName() == RequireKotlinConstants.FQ_NAME
            }
            .mapNotNull(::serializeVersionRequirementFromRequireKotlin)
            .map(::get)

    private fun MutableVersionRequirementTable.writeVersionRequirement(languageFeature: LanguageFeature): Int {
        return writeLanguageVersionRequirement(languageFeature, this)
    }

    private fun MutableVersionRequirementTable.writeVersionRequirementDependingOnCoroutinesVersion(): Int =
        writeVersionRequirement(LanguageFeature.ReleaseCoroutines)

    private fun serializeVersionRequirementFromRequireKotlin(annotation: FirAnnotationCall): ProtoBuf.VersionRequirement.Builder? {
        val args = annotation.argumentList

        val versionString = (args[RequireKotlinConstants.VERSION]?.toConstantValue() as? StringValue)?.value ?: return null
        val matchResult = RequireKotlinConstants.VERSION_REGEX.matchEntire(versionString) ?: return null

        val major = matchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = matchResult.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = matchResult.groupValues.getOrNull(4)?.toIntOrNull() ?: 0

        val proto = ProtoBuf.VersionRequirement.newBuilder()
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { proto.version = it },
            writeVersionFull = { proto.versionFull = it }
        )

        val message = (args[RequireKotlinConstants.MESSAGE]?.toConstantValue() as? StringValue)?.value
        if (message != null) {
            proto.message = stringTable.getStringIndex(message)
        }

        when ((args[RequireKotlinConstants.LEVEL]?.toConstantValue() as? EnumValue)?.enumEntryName?.asString()) {
            DeprecationLevel.ERROR.name -> {
                // ERROR is the default level
            }
            DeprecationLevel.WARNING.name -> proto.level = ProtoBuf.VersionRequirement.Level.WARNING
            DeprecationLevel.HIDDEN.name -> proto.level = ProtoBuf.VersionRequirement.Level.HIDDEN
        }

        when ((args[RequireKotlinConstants.VERSION_KIND]?.toConstantValue() as? EnumValue)?.enumEntryName?.asString()) {
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION.name -> {
                // LANGUAGE_VERSION is the default kind
            }
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            ProtoBuf.VersionRequirement.VersionKind.API_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.API_VERSION
        }

        val errorCode = (args[RequireKotlinConstants.ERROR_CODE]?.toConstantValue() as? IntValue)?.value
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
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeApproximator
            )

        @JvmStatic
        fun createForLambda(
            session: FirSession,
            scopeSession: ScopeSession,
            extension: FirSerializerExtension,
            typeApproximator: AbstractTypeApproximator,
        ): FirElementSerializer =
            FirElementSerializer(
                session, scopeSession, null,
                Interner(), extension, MutableTypeTable(),
                versionRequirementTable = null, serializeTypeTableToFunction = true,
                typeApproximator
            )

        @JvmStatic
        fun create(
            session: FirSession,
            scopeSession: ScopeSession,
            klass: FirClass,
            extension: FirSerializerExtension,
            parentSerializer: FirElementSerializer?,
            typeApproximator: AbstractTypeApproximator,
        ): FirElementSerializer {
            val parentClassId = klass.symbol.classId.outerClassId
            val parent = if (parentClassId != null && !parentClassId.isLocal) {
                val parentClass = session.symbolProvider.getClassLikeSymbolByFqName(parentClassId)!!.fir as FirRegularClass
                parentSerializer ?: create(session, scopeSession, parentClass, extension, null, typeApproximator)
            } else {
                createTopLevel(session, scopeSession, extension, typeApproximator)
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
                typeApproximator
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
