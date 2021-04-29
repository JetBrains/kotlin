/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider.Companion.CLONE
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider.Companion.CLONEABLE_CLASS_ID
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
    defaultAnnotationDeserializer: AbstractAnnotationDeserializer?,
    scopeProvider: FirScopeProvider,
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
        isInline = Flags.IS_INLINE_CLASS.get(classProto.flags)
        isExternal = Flags.IS_EXTERNAL_CLASS.get(classProto.flags)
        isFun = Flags.IS_FUN_INTERFACE.get(classProto.flags)
    }
    val isSealed = modality == Modality.SEALED
    val annotationDeserializer = defaultAnnotationDeserializer ?: FirBuiltinAnnotationDeserializer(session)
    val context =
        parentContext?.childContext(
            classProto.typeParameterList,
            nameResolver,
            TypeTable(classProto.typeTable),
            classId.relativeClassName,
            containerSource,
            symbol,
            annotationDeserializer,
            status.isInner
        ) ?: FirDeserializationContext.createForClass(
            classId,
            classProto,
            nameResolver,
            moduleData,
            annotationDeserializer,
            FirConstDeserializer(session, (containerSource as? KotlinJvmBinarySourceElement)?.binaryClass),
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

        val superTypesDeserialized = classProto.supertypes(context.typeTable).map { supertypeProto ->
            typeDeserializer.simpleType(supertypeProto, ConeAttributes.Empty)
        }

        superTypesDeserialized.mapNotNullTo(superTypeRefs) {
            if (it == null) return@mapNotNullTo null
            buildResolvedTypeRef { type = it }
        }

        addDeclarations(
            classProto.functionList.map {
                classDeserializer.loadFunction(it, classProto, symbol)
            }
        )

        addDeclarations(
            classProto.propertyList.map {
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
            classProto.enumEntryList.mapNotNull { enumEntryProto ->
                val enumEntryName = nameResolver.getName(enumEntryProto.name)

                val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), emptyArray(), false)
                val property = buildEnumEntry {
                    this.moduleData = moduleData
                    this.origin = FirDeclarationOrigin.Library
                    returnTypeRef = buildResolvedTypeRef { type = enumType }
                    name = enumEntryName
                    this.symbol = FirVariableSymbol(CallableId(classId, enumEntryName))
                    this.status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    ).apply {
                        isStatic = true
                    }
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                }.apply {
                    containingClassAttr = context.dispatchReceiver!!.lookupTag
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
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver)
        addSerializableIfNeeded(classId)

        declarations.sortWith(object : Comparator<FirDeclaration> {
            override fun compare(a: FirDeclaration, b: FirDeclaration): Int {
                // Reorder members based on their type and name only.
                // See FE 1.0's [DeserializedMemberScope#addMembers].
                if (a is FirMemberDeclaration && b is FirMemberDeclaration) {
                    return FirMemberDeclarationComparator.TypeAndNameComparator.compare(a, b)
                }
                return 0
            }
        })
    }.also {
        if (isSealed) {
            val inheritors = classProto.sealedSubclassFqNameList.map { nameIndex ->
                ClassId.fromString(nameResolver.getQualifiedClassName(nameIndex))
            }
            it.setSealedClassInheritors(inheritors)
        }
        (it.annotations as MutableList<FirAnnotationCall>) +=
            context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)

        it.versionRequirementsTable = context.versionRequirementTable

        it.sourceElement = containerSource
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
            ConeClassLikeLookupTagImpl(JAVA_IO_SERIALIZABLE),
            typeArguments = emptyArray(),
            isNullable = false
        )
    }
}

private fun FirRegularClassBuilder.addCloneForArrayIfNeeded(classId: ClassId, dispatchReceiver: ConeClassLikeType?) {
    if (classId.packageFqName != StandardNames.BUILT_INS_PACKAGE_FQ_NAME) return
    if (classId.shortClassName !in ARRAY_CLASSES) return
    superTypeRefs += buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(CLONEABLE_CLASS_ID),
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
                ConeClassLikeLookupTagImpl(classId),
                typeArguments = typeArguments,
                isNullable = false
            )
        }
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public).apply {
            isOverride = true
        }
        name = CLONE
        symbol = FirNamedFunctionSymbol(CallableId(classId, CLONE))
        dispatchReceiverType = dispatchReceiver!!
    }
}
