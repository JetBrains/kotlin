package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.SimpleType

object NullFlexibleTypeDeserializer : FlexibleTypeDeserializer {

    override fun create(
            proto: ProtoBuf.Type,
            flexibleId: String,
            lowerBound: SimpleType,
            upperBound: SimpleType) = error("Illegal use of flexible type deserializer.")
}
