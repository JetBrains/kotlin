/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.addDeclarations
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.generateValueOfFunction
import org.jetbrains.kotlin.fir.generateValuesFunction
import org.jetbrains.kotlin.fir.resolve.impl.FirClonableSymbolProvider.Companion.CLONABLE_CLASS_ID
import org.jetbrains.kotlin.fir.resolve.impl.FirClonableSymbolProvider.Companion.CLONE
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
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
    defaultAnnotationDeserializer: AbstractAnnotationDeserializer?,
    scopeProvider: KotlinScopeProvider,
    parentContext: FirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    deserializeNestedClass: (ClassId, FirDeserializationContext) -> FirRegularClassSymbol?
) {
    val flags = classProto.flags
    val kind = Flags.CLASS_KIND.get(flags)
    val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
    val status = FirDeclarationStatusImpl(
        ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
        modality
    ).apply {
        isExpect = Flags.IS_EXPECT_CLASS.get(flags)
        isActual = false
        isCompanion = kind == ProtoBuf.Class.Kind.COMPANION_OBJECT
        isInner = Flags.IS_INNER.get(flags)
        isData = Flags.IS_DATA.get(classProto.flags)
        isInline = Flags.IS_INLINE_CLASS.get(classProto.flags)
    }
    val isSealed = modality == Modality.SEALED
    val classBuilder = if (isSealed) FirSealedClassBuilder() else FirClassImplBuilder()
    val context =
        parentContext?.childContext(
            classProto.typeParameterList,
            nameResolver,
            TypeTable(classProto.typeTable),
            classId.relativeClassName,
            status.isInner
        ) ?: FirDeserializationContext.createForClass(
            classId, classProto, nameResolver, session,
            defaultAnnotationDeserializer ?: FirBuiltinAnnotationDeserializer(session),
            containerSource
        )
    classBuilder.apply {
        this.session = session
        origin = FirDeclarationOrigin.Library
        name = classId.shortClassName
        this.status = status
        classKind = ProtoEnumFlags.classKind(kind)
        this.scopeProvider = scopeProvider
        this.symbol = symbol

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }
        if (status.isInner)
            typeParameters += parentContext?.allTypeParameters?.map { buildOuterClassTypeParameterRef { this.symbol = it } }.orEmpty()
//        annotations += context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)

        val typeDeserializer = context.typeDeserializer
        val classDeserializer = context.memberDeserializer

        val superTypesDeserialized = classProto.supertypes(context.typeTable).map { supertypeProto ->
            typeDeserializer.simpleType(supertypeProto)
        }// TODO: + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

        superTypesDeserialized.mapNotNullTo(superTypeRefs) {
            if (it == null) return@mapNotNullTo null
            buildResolvedTypeRef { type = it }
        }

        addDeclarations(classProto.functionList.map(classDeserializer::loadFunction))
        addDeclarations(classProto.propertyList.map(classDeserializer::loadProperty))

        addDeclarations(
            classProto.constructorList.map {
                classDeserializer.loadConstructor(it, this)
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
                    this.session = session
                    origin = FirDeclarationOrigin.Library
                    returnTypeRef = buildResolvedTypeRef { type = enumType }
                    name = enumEntryName
                    this.symbol = FirVariableSymbol(CallableId(classId, enumEntryName))
                    this.status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
                        isStatic = true
                    }
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                }

                property
            }
        )

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(session, classId.packageFqName, classId.relativeClassName)
            generateValueOfFunction(session, classId.packageFqName, classId.relativeClassName)
        }

        if (isSealed) {
            classProto.sealedSubclassFqNameList.mapTo((this as FirSealedClassBuilder).inheritors) {
                ClassId.fromString(nameResolver.getQualifiedClassName(it))
            }
        }
        addCloneForArrayIfNeeded(classId)
        addSerializableIfNeeded(classId)
    }.build().also {
        (it.annotations as MutableList<FirAnnotationCall>) += context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)
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

private fun AbstractFirRegularClassBuilder.addSerializableIfNeeded(classId: ClassId) {
    if (!JvmBuiltInsSettings.isSerializableInJava(classId.asSingleFqName().toUnsafe())) return
    superTypeRefs += buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(JAVA_IO_SERIALIZABLE),
            typeArguments = emptyArray(),
            isNullable = false
        )
    }
}

private fun AbstractFirRegularClassBuilder.addCloneForArrayIfNeeded(classId: ClassId) {
    if (classId.packageFqName != KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) return
    if (classId.shortClassName !in ARRAY_CLASSES) return
    superTypeRefs += buildResolvedTypeRef {
        type = ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(CLONABLE_CLASS_ID),
            typeArguments = emptyArray(),
            isNullable = false
        )
    }
    declarations += buildSimpleFunction {
        session = this@addCloneForArrayIfNeeded.session
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
        status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
            isOverride = true
        }
        name = CLONE
        symbol = FirNamedFunctionSymbol(CallableId(classId, CLONE))
    }
}
