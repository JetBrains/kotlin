/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class LibraryClassSymbol(
    val classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    val typeDeserializer: FirTypeDeserializer
) : ConeClassSymbol {

    override val kind: ClassKind = ProtoEnumFlags.classKind(Flags.CLASS_KIND[classProto.flags])

    override val typeParameters: List<ConeTypeParameterSymbol> by lazy { typeDeserializer.ownTypeParameters }
    override val classId: ClassId = nameResolver.getClassId(classProto.fqName)

    val typeTable = TypeTable(classProto.typeTable)

    override val superTypes: List<ConeClassLikeType>
        get() {
            val result = classProto.supertypes(typeTable).map { supertypeProto ->
                typeDeserializer.classLikeType(supertypeProto)
            }// TODO: + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

            return result.filterNotNull()
        }
}