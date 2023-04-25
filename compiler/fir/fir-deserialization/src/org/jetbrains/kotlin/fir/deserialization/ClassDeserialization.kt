/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.moduleName
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.serialization.deserialization.loadValueClassRepresentation

fun deserializeClassToSymbol(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    symbol: FirRegularClassSymbol,
    nameResolver: NameResolver,
    session: FirSession,
    moduleData: FirModuleData,
    defaultAnnotationDeserializer: AbstractAnnotationDeserializer?,
    scopeProvider: FirScopeProvider,
    serializerExtensionProtocol: SerializerExtensionProtocol,
    parentContext: FirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    origin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
    deserializeNestedClass: (ClassId, FirDeserializationContext) -> FirRegularClassSymbol?
) {
    val flags = classProto.flags
    val kind = Flags.CLASS_KIND.get(flags)
    val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
    val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
    val status = FirResolvedDeclarationStatusImpl(
        visibility,
        modality,
        visibility.toEffectiveVisibility(parentContext?.outerClassSymbol, forClass = true)
    ).apply {
        isExpect = Flags.IS_EXPECT_CLASS.get(flags)
        isActual = false
        isCompanion = kind == ProtoBuf.Class.Kind.COMPANION_OBJECT
        isInner = Flags.IS_INNER.get(flags)
        isData = Flags.IS_DATA.get(classProto.flags)
        isInline = Flags.IS_VALUE_CLASS.get(classProto.flags)
        isExternal = Flags.IS_EXTERNAL_CLASS.get(classProto.flags)
        isFun = Flags.IS_FUN_INTERFACE.get(classProto.flags)
    }
    val isSealed = modality == Modality.SEALED
    val annotationDeserializer = defaultAnnotationDeserializer ?: FirBuiltinAnnotationDeserializer(session)
    val jvmBinaryClass = (containerSource as? KotlinJvmBinarySourceElement)?.binaryClass
    val constDeserializer = if (jvmBinaryClass != null) {
        FirJvmConstDeserializer(session, jvmBinaryClass, serializerExtensionProtocol)
    } else {
        FirConstDeserializer(session, serializerExtensionProtocol)
    }
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
                status.isCompanion || jvmBinaryClass == null -> parentContext.constDeserializer
                else -> constDeserializer // jvmBinaryClass != null => FirJvmConstDeserializer will be used
            },
            status.isInner
        ) ?: FirDeserializationContext.createForClass(
            classId,
            classProto,
            nameResolver,
            moduleData,
            annotationDeserializer,
            constDeserializer,
            containerSource,
            symbol
        )
    if (status.isCompanion) {
        parentContext?.let {
            context.annotationDeserializer.inheritAnnotationInfo(it.annotationDeserializer)
        }
    }
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

        addDeclarations(
            classProto.propertiesInOrder(context).map {
                classDeserializer.loadProperty(it, classProto, symbol)
            }
        )

        addDeclarations(
            classProto.constructorList.map {
                classDeserializer.loadConstructor(it, classProto, this)
            }
        )

        addDeclarations(
            classProto.nestedClassNameList.mapNotNull { nestedNameId ->
                val nestedClassId = classId.createNestedClassId(Name.identifier(nameResolver.getString(nestedNameId)))
                deserializeNestedClass(nestedClassId, context)?.fir
            }
        )

        addDeclarations(
            classProto.typeAliasList.mapNotNull(classDeserializer::loadTypeAlias)
        )

        addDeclarations(
            classProto.enumEntryList.mapNotNull { enumEntryProto ->
                val enumEntryName = nameResolver.getName(enumEntryProto.name)

                val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), emptyArray(), false)
                val property = buildEnumEntry {
                    this.moduleData = moduleData
                    this.origin = FirDeclarationOrigin.Library
                    returnTypeRef = buildResolvedTypeRef { type = enumType }
                    name = enumEntryName
                    this.symbol = FirEnumEntrySymbol(CallableId(classId, enumEntryName))
                    this.status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    ).apply {
                        isStatic = true
                    }
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                }.apply {
                    containingClassForStaticMemberAttr = context.dispatchReceiver!!.lookupTag
                }

                property
            }
        )

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName)
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver)
        session.deserializedClassConfigurator?.run {
            configure(classId)
        }

        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol

        contextReceivers.addAll(classDeserializer.createContextReceiversForClass(classProto))
    }.apply {
        if (isSealed) {
            val inheritors = classProto.sealedSubclassFqNameList.map { nameIndex ->
                ClassId.fromString(nameResolver.getQualifiedClassName(nameIndex))
            }
            setSealedClassInheritors(inheritors)
        }

        valueClassRepresentation =
            classProto.loadValueClassRepresentation(context.nameResolver, context.typeTable, context.typeDeserializer::simpleType) { name ->
                val member = declarations.singleOrNull { it is FirProperty && it.receiverParameter == null && it.name == name }
                (member as FirProperty?)?.returnTypeRef?.coneTypeSafe()
            } ?: computeValueClassRepresentation(this, session)

        replaceAnnotations(
            context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)
        )

        versionRequirementsTable = context.versionRequirementTable

        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        classProto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let { idx ->
            moduleName = nameResolver.getString(idx)
        }
        session.deserializedClassConfigurator?.run {
            configure(classId)
        }

        if (!Flags.HAS_ENUM_ENTRIES.get(flags)) {
            hasNoEnumEntriesAttr = true
        }
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

private val JAVA_IO_SERIALIZABLE = ClassId.topLevel(FqName("java.io.Serializable"))

private fun FirRegularClassBuilder.addSerializableIfNeeded(classId: ClassId) {
    if (!JvmBuiltInsSignatures.isSerializableInJava(classId.asSingleFqName().toUnsafe())) return
    superTypeRefs += buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            JAVA_IO_SERIALIZABLE.toLookupTag(),
            typeArguments = emptyArray(),
            isNullable = false
        )
    }
}

private fun FirRegularClassBuilder.addCloneForArrayIfNeeded(classId: ClassId, dispatchReceiver: ConeClassLikeType?) {
    if (classId.packageFqName != StandardClassIds.BASE_KOTLIN_PACKAGE) return
    if (classId.shortClassName !in ARRAY_CLASSES) return
    superTypeRefs += buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            StandardClassIds.Cloneable.toLookupTag(),
            typeArguments = emptyArray(),
            isNullable = false
        )
    }
    declarations += buildSimpleFunction {
        moduleData = this@addCloneForArrayIfNeeded.moduleData
        origin = FirDeclarationOrigin.Library
        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        returnTypeRef = buildResolvedTypeRef {
            val typeArguments = if (classId.shortClassName == ARRAY) {
                arrayOf(
                    ConeTypeParameterTypeImpl(
                        ConeTypeParameterLookupTag(this@addCloneForArrayIfNeeded.typeParameters.first().symbol), isNullable = false
                    )
                )
            } else {
                emptyArray()
            }
            type = ConeClassLikeTypeImpl(
                classId.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = false
            )
        }
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public).apply {
            isOverride = true
        }
        name = StandardClassIds.Callables.clone.callableName
        symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        dispatchReceiverType = dispatchReceiver!!
    }
}

abstract class DeserializedClassConfigurator(val session: FirSession) : FirSessionComponent {
    open fun FirRegularClassBuilder.configure(classId: ClassId) {}

    open fun FirRegularClass.configure(classId: ClassId) {}
}

class JvmDeserializedClassConfigurator(session: FirSession) : DeserializedClassConfigurator(session) {
    override fun FirRegularClassBuilder.configure(classId: ClassId) {
        addSerializableIfNeeded(classId)
    }
}

private fun ProtoBuf.ClassOrBuilder.propertiesInOrder(context: FirDeserializationContext): List<ProtoBuf.Property> {
    val properties = propertyList
    val versionRequirements = VersionRequirement.create(this, context.nameResolver, context.versionRequirementTable)
    if (versionRequirements.any { it.version.major >= 2 }) return properties
    val order = getExtension(SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder)
        .takeIf { it.isNotEmpty() }
        ?.toSet()
        ?: return properties
    val propertiesByName = properties.groupBy { it.name }
    val orderedProperties = order.flatMap { propertiesByName[it] ?: emptyList() }
    // non-serializable properties are not saved in SerializationPluginMetadataExtensions, so we need to pick up them if any
    return if (orderedProperties.size == properties.size) {
        orderedProperties
    } else {
        orderedProperties + properties.filter { it.name !in order }
    }
}

val FirSession.deserializedClassConfigurator: DeserializedClassConfigurator? by FirSession.nullableSessionComponentAccessor()
