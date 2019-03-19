/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
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
import org.jetbrains.kotlin.types.Variance

fun deserializeClassToSymbol(
    classId: ClassId,
    classProto: ProtoBuf.Class,
    symbol: FirClassSymbol,
    nameResolver: NameResolver,
    session: FirSession
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
        for (typeParameter in classProto.typeParameterList) {
            typeParameters += createTypeParameterSymbol(nameResolver.getName(typeParameter.name), session).fir
        }
        //addAnnotationsFrom(classProto) ? TODO

        val typeTable = TypeTable(classProto.typeTable)
        val typeDeserializer = FirTypeDeserializer(
            nameResolver,
            typeTable,
            classProto.typeParameterList,
            null
        )

        val superTypesDeserialized = classProto.supertypes(typeTable).map { supertypeProto ->
            typeDeserializer.simpleType(supertypeProto)
        }// TODO: + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

        superTypesDeserialized.mapNotNullTo(superTypeRefs) {
            if (it == null) return@mapNotNullTo null
            FirResolvedTypeRefImpl(session, null, it, false, emptyList())
        }

        val classDeserializer =
            FirDeserializationContext
                .createForClass(classId, classProto, nameResolver, session)
                .memberDeserializer

        // TODO: properties + nested classes
        declarations += classProto.functionList.map(classDeserializer::loadFunction)
    }
}

private fun createTypeParameterSymbol(name: Name, session: FirSession): FirTypeParameterSymbol {
    val firSymbol = FirTypeParameterSymbol()
    FirTypeParameterImpl(session, null, firSymbol, name, variance = Variance.INVARIANT, isReified = false)
    return firSymbol
}
