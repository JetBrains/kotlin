/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirEnumEntryImpl
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getName

fun deserializeClassToSymbol(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    symbol: FirClassSymbol,
    nameResolver: NameResolver,
    session: FirSession,
    defaultAnnotationDeserializer: AbstractAnnotationDeserializer?,
    parentContext: FirDeserializationContext? = null,
    deserializeNestedClass: (ClassId, FirDeserializationContext) -> FirClassSymbol?
) {
    val flags = classProto.flags
    val kind = Flags.CLASS_KIND.get(flags)
    FirClassImpl(
        session, null, symbol, classId.shortClassName,
        ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
        ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
        Flags.IS_EXPECT_CLASS.get(flags), false,
        ProtoEnumFlags.classKind(kind),
        Flags.IS_INNER.get(flags),
        kind == ProtoBuf.Class.Kind.COMPANION_OBJECT,
        Flags.IS_DATA.get(classProto.flags),
        Flags.IS_INLINE_CLASS.get(classProto.flags)
    ).apply {
        resolvePhase = FirResolvePhase.DECLARATIONS
        val context =
            parentContext?.childContext(
                classProto.typeParameterList,
                nameResolver,
                TypeTable(classProto.typeTable),
                classId.relativeClassName
            ) ?: FirDeserializationContext.createForClass(
                classId, classProto, nameResolver, session,
                defaultAnnotationDeserializer ?: FirBuiltinAnnotationDeserializer(session)
            )
        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }
        annotations += context.annotationDeserializer.loadClassAnnotations(classProto, context.nameResolver)

        val typeDeserializer = context.typeDeserializer
        val classDeserializer = context.memberDeserializer

        val superTypesDeserialized = classProto.supertypes(context.typeTable).map { supertypeProto ->
            typeDeserializer.simpleType(supertypeProto)
        }// TODO: + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

        superTypesDeserialized.mapNotNullTo(superTypeRefs) {
            if (it == null) return@mapNotNullTo null
            FirResolvedTypeRefImpl(null, it)
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
                val enumEntryId = classId.createNestedClassId(enumEntryName)

                val symbol = FirClassSymbol(enumEntryId)
                FirEnumEntryImpl(session, null, symbol, enumEntryId.shortClassName).apply {
                    resolvePhase = FirResolvePhase.DECLARATIONS
                    superTypeRefs += FirResolvedTypeRefImpl(
                        null,
                        ConeClassTypeImpl(ConeClassLikeLookupTagImpl(classId), emptyArray(), false),
                        emptyList()
                    )
                }


                symbol.fir
            }
        )
    }
}

