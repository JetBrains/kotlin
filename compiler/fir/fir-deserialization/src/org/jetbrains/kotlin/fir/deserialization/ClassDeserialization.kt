/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusWithLazyEffectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.addDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.declarations.utils.moduleName
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.transformers.setLazyPublishedVisibility
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName

fun deserializeClassToSymbol(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    symbol: FirRegularClassSymbol,
    nameResolver: NameResolver,
    session: FirSession,
    moduleData: FirModuleData,
    defaultAnnotationDeserializer: AnnotationDeserializer?,
    kdocDeserializer: FirKDocDeserializer,
    flexibleTypeFactory: FirTypeDeserializer.FlexibleTypeFactory,
    scopeProvider: FirScopeProvider,
    serializerExtensionProtocol: SerializerExtensionProtocol,
    parentContext: FirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
    deserializeNestedClass: (ClassId, FirDeserializationContext) -> FirRegularClassSymbol?,
    deserializeNestedTypeAlias: (ClassId, FirNestedTypeAliasDeserializationContext) -> FirTypeAliasSymbol?,
) {
    val flags = classProto.flags
    val kind = Flags.CLASS_KIND.get(flags)
    val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
    val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
    val effectiveVisibility = visibility.toLazyEffectiveVisibility(
        owner = parentContext?.outerClassSymbol,
        session,
        forClass = true
    )
    val status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(visibility, modality, effectiveVisibility).apply {
        isExpect = Flags.IS_EXPECT_CLASS.get(flags)
        isActual = false
        isCompanion = kind == ProtoBuf.Class.Kind.COMPANION_OBJECT
        isInner = Flags.IS_INNER.get(flags)
        isData = Flags.IS_DATA.get(classProto.flags)
        isValue = Flags.IS_VALUE_CLASS.get(classProto.flags)

        // TODO: KT-90593. Expect inline value classes without constructors
        //  (AllowExpectValueClassesWithNoPrimaryConstructor) are loaded as
        //  full value classes.
        isInline = isValue && (session.deserializationExtension?.isMaybeFullValueClass(containerSource) == false
                || classProto.hasInlineClassUnderlyingPropertyName())
        isExternal = Flags.IS_EXTERNAL_CLASS.get(classProto.flags)
        isFun = Flags.IS_FUN_INTERFACE.get(classProto.flags)
        isRichError = Flags.IS_RICH_ERROR.get(classProto.flags)
    }
    val isSealed = modality == Modality.SEALED
    val annotationDeserializer = defaultAnnotationDeserializer ?: FirBuiltinAnnotationDeserializer(session)
    val platformConstDeserializer =
        session.deserializationExtension?.createConstDeserializer(containerSource, session, serializerExtensionProtocol)
    val constDeserializer = platformConstDeserializer ?: FirConstDeserializer(serializerExtensionProtocol)
    val context =
        parentContext?.childContext(
            classProto.typeParameterList,
            containingDeclarationSymbol = symbol,
            nameResolver,
            TypeTable(classProto.typeTable),
            classId.relativeClassName,
            containerSource,
            outerClassSymbol = symbol,
            annotationDeserializer,
            when {
                status.isCompanion || platformConstDeserializer == null -> parentContext.constDeserializer
                else -> constDeserializer // platformConstDeserializer != null => FirJvmConstDeserializer will be used on JVM
            },
            status.isInner
        ) ?: FirDeserializationContext.createForClass(
            classId,
            classProto,
            nameResolver,
            moduleData,
            annotationDeserializer,
            flexibleTypeFactory,
            constDeserializer,
            kdocDeserializer,
            containerSource,
            symbol,
            status.effectiveVisibility
        )
    if (status.isCompanion) {
        parentContext?.let {
            context.annotationDeserializer.inheritAnnotationInfo(it.annotationDeserializer)
        }
    }

    val versionRequirements = VersionRequirement.create(classProto, context)

    buildRegularClass {
        this.moduleData = moduleData
        this.origin = origin
        name = classId.shortClassName
        this.status = status
        classKind = ProtoEnumFlags.classKind(kind)
        this.scopeProvider = scopeProvider
        this.symbol = symbol

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }
        if (status.isInner)
            typeParameters += parentContext?.allTypeParameters?.map { buildOuterClassTypeParameterRef { this.symbol = it } }.orEmpty()

        val typeDeserializer = context.typeDeserializer
        val classDeserializer = context.memberDeserializer

        classProto.supertypes(context.typeTable).mapTo(superTypeRefs, typeDeserializer::typeRef)

        addDeclarations(
            classProto.functionList.map {
                classDeserializer.loadFunction(it, classProto, symbol)
            }
        )

        val propertiesOrderFromExtension = classProto.getPropertyOrderFromMetadataExtension()
        addDeclarations(
            classProto.propertiesInOrder(versionRequirements, propertiesOrderFromExtension).map { propertyProto ->
                classDeserializer.loadProperty(propertyProto, classProto, symbol).also { property ->
                    if (propertyProto.name in propertiesOrderFromExtension) {
                        property.registeredInSerializationPluginMetadataExtension = true
                    }
                }
            }
        )

        addDeclarations(
            classProto.constructorList.map {
                classDeserializer.loadConstructor(it, classProto, this)
            }
        )

        fun createNestedClassId(nameId: Int): ClassId {
            return classId.createNestedClassId(Name.identifier(nameResolver.getString(nameId)))
        }

        addDeclarations(
            classProto.nestedClassNameList.mapNotNull {
                deserializeNestedClass(createNestedClassId(it), context)?.fir
            }
        )

        addDeclarations(
            classProto.typeAliasList.mapNotNull {
                deserializeNestedTypeAlias(
                    createNestedClassId(it.name),
                    FirNestedTypeAliasDeserializationContext(classDeserializer, it, scopeProvider)
                )?.fir
            }
        )

        addDeclarations(
            classProto.enumEntryList.mapNotNull { enumEntryProto ->
                val enumEntryName = nameResolver.getName(enumEntryProto.name)

                val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), ConeTypeProjection.EMPTY_ARRAY, false)
                val property = buildEnumEntry {
                    this.moduleData = moduleData
                    this.origin = FirDeclarationOrigin.Library
                    returnTypeRef = buildResolvedTypeRef { coneType = enumType }
                    name = enumEntryName
                    this.symbol = FirEnumEntrySymbol(CallableId(classId, enumEntryName))
                    this.status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    ).apply {
                        isStatic = true
                    }
                    isLocal = false
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    attributes.applyKDoc(context.kdocDeserializer.loadEnumEntryKDoc(enumEntryProto))
                }.apply {
                    containingClassForStaticMemberAttr = context.dispatchReceiver!!.lookupTag
                    replaceAnnotations(
                        context.annotationDeserializer.loadEnumEntryAnnotations(classId, enumEntryProto, context.nameResolver)
                    )
                }

                property
            }
        )

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = origin,
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName, origin = origin)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName, origin = origin)
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver, session)
        session.deserializationExtension?.run {
            configureDeserializedClass(classId)
        }

        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol

        contextParameters.addAll(classDeserializer.createContextParametersForClass(classProto, origin, symbol))

        applyKDoc(context.kdocDeserializer.loadClassKDoc(classProto))
    }.apply {
        if (isSealed) {
            val inheritors = classProto.sealedSubclassFqNameList.map { nameIndex ->
                ClassId.fromString(nameResolver.getQualifiedClassName(nameIndex))
            }
            setSealedClassInheritors(inheritors)
        }

        if (isValue) {
            valueClassRepresentation =
                classProto.loadValueClassRepresentation(
                    isInline,
                    context,
                    declarations,
                )
        }

        replaceAnnotations(
            context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)
        )

        this.versionRequirements = versionRequirements

        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        session.deserializationExtension?.loadModuleName(classProto, nameResolver)?.let {
            moduleName = it
        }

        if (!Flags.HAS_ENUM_ENTRIES.get(flags)) {
            hasNoEnumEntriesAttr = true
        }
        deserializeCompilerPluginMetadata(context, classProto, ProtoBuf.Class::getCompilerPluginDataList)
        setLazyPublishedVisibility(session)
    }
}

private val ARRAY = Name.identifier("Array")
private val ARRAY_CLASSES: Set<Name> = setOf(
    ARRAY,
    Name.identifier("ByteArray"),
    Name.identifier("CharArray"),
    Name.identifier("ShortArray"),
    Name.identifier("IntArray"),
    Name.identifier("LongArray"),
    Name.identifier("FloatArray"),
    Name.identifier("DoubleArray"),
    Name.identifier("BooleanArray"),
)

fun FirRegularClassBuilder.addCloneForArrayIfNeeded(classId: ClassId, dispatchReceiver: ConeClassLikeType?, session: FirSession) {
    if (classId.packageFqName != StandardClassIds.BASE_KOTLIN_PACKAGE) return
    if (classId.shortClassName !in ARRAY_CLASSES) return
    if (session.getRegularClassSymbolByClassId(StandardClassIds.Cloneable) == null) return
    superTypeRefs += buildResolvedTypeRef {
        coneType = ConeClassLikeTypeImpl(
            StandardClassIds.Cloneable.toLookupTag(),
            typeArguments = ConeTypeProjection.EMPTY_ARRAY,
            isMarkedNullable = false
        )
    }
    declarations += buildNamedFunction {
        moduleData = this@addCloneForArrayIfNeeded.moduleData
        origin = FirDeclarationOrigin.Library
        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        returnTypeRef = buildResolvedTypeRef {
            val typeArguments = if (classId.shortClassName == ARRAY) {
                arrayOf(
                    ConeTypeParameterType(
                        ConeTypeParameterLookupTag(this@addCloneForArrayIfNeeded.typeParameters.first().symbol), isMarkedNullable = false
                    )
                )
            } else {
                emptyArray()
            }
            coneType = ConeClassLikeTypeImpl(
                classId.toLookupTag(),
                typeArguments = typeArguments,
                isMarkedNullable = false
            )
        }
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public).apply {
            isOverride = true
        }
        isLocal = false
        name = StandardClassIds.Callables.clone.callableName
        symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        dispatchReceiverType = dispatchReceiver!!
    }
}

private fun ProtoBuf.ClassOrBuilder.getPropertyOrderFromMetadataExtension(): Set<Int> {
    return getExtension(SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder).toSet()
}

private fun ProtoBuf.ClassOrBuilder.propertiesInOrder(
    versionRequirements: List<VersionRequirement>,
    orderFromExtension: Set<Int>,
): List<ProtoBuf.Property> {
    val properties = propertyList
    if (versionRequirements.any { it.version.major >= 2 }) return properties
    if (orderFromExtension.isEmpty()) return properties
    val propertiesByName = properties.groupBy { it.name }
    val orderedProperties = orderFromExtension.flatMap { propertiesByName[it] ?: emptyList() }
    // non-serializable properties are not saved in SerializationPluginMetadataExtensions, so we need to pick up them if any
    return if (orderedProperties.size == properties.size) {
        orderedProperties
    } else {
        orderedProperties + properties.filter { it.name !in orderFromExtension }
    }
}

private fun ProtoBuf.Class.loadValueClassRepresentation(
    isInline: Boolean,
    context: FirDeserializationContext,
    declarations: List<FirDeclaration>,
): ValueClassRepresentation<ConeRigidType> {
    val primaryConstructor = declarations.filterIsInstance<FirConstructor>().singleOrNull { it.isPrimary }

    if (!isInline) {
        val modality = Flags.MODALITY.get(flags)
        val isAbstractOrSealed = modality == ProtoBuf.Modality.ABSTRACT || modality == ProtoBuf.Modality.SEALED
        val fields = if (isAbstractOrSealed) {
            null
        } else {
            // jvm-abi-gen strips a private primary constructor, and its parameters aren't needed outside the class anyway.
            primaryConstructor?.valueParameters?.map { it.name to it.returnTypeRef.coneType as ConeRigidType }.orEmpty()
        }
        return FullValueClassRepresentation(fields)
    }

    val className = context.nameResolver.getQualifiedClassName(fqName)

    if (hasInlineClassUnderlyingPropertyName()) {
        val propertyName = context.nameResolver.getName(inlineClassUnderlyingPropertyName)

        // inlineClassUnderlyingType is only written to metadata if the property is not publicAPI
        val propertyType = inlineClassUnderlyingType(context.typeTable)?.let(context.typeDeserializer::rigidType)
            ?: declarations.filterIsInstance<FirProperty>().singleOrNull {
                it.receiverParameter == null && it.contextParameters.isEmpty() && it.name == propertyName && !it.status.isStatic
            }?.returnTypeRef?.coneType as? ConeRigidType
            ?: error("cannot determine underlying type for value class $className with property $propertyName")

        return InlineClassRepresentation(propertyName, propertyType)
    }

    // Versions before 1.5 did not write inlineClassUnderlyingPropertyName.
    // We can legitimately throw here if inline class was declared with private constructor
    // or private property.
    val parameter = primaryConstructor?.valueParameters?.singleOrNull()
        ?: error("cannot determine underlying property for value class $className")
    return InlineClassRepresentation(parameter.name, parameter.returnTypeRef.coneType as ConeRigidType)
}

val FirSession.deserializationExtension: FirDeserializationExtension? by FirSession.nullableSessionComponentAccessor()

private object RegisteredInSerializationPluginMetadataExtensionKey : FirDeclarationDataKey()

private var FirProperty.registeredInSerializationPluginMetadataExtension: Boolean?
        by FirDeclarationDataRegistry.data(RegisteredInSerializationPluginMetadataExtensionKey)

val FirPropertySymbol.registeredInSerializationPluginMetadataExtension: Boolean
    get() = fir.registeredInSerializationPluginMetadataExtension ?: false
