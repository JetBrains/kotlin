package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object KonanSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply { KonanProtoBuf.registerAllExtensions(this) },
        KonanProtoBuf.packageFqName,
        KonanProtoBuf.constructorAnnotation,
        KonanProtoBuf.classAnnotation,
        KonanProtoBuf.functionAnnotation,
        KonanProtoBuf.propertyAnnotation,
        KonanProtoBuf.propertyGetterAnnotation,
        KonanProtoBuf.propertySetterAnnotation,
        KonanProtoBuf.enumEntryAnnotation,
        KonanProtoBuf.compileTimeValue,
        KonanProtoBuf.parameterAnnotation,
        KonanProtoBuf.typeAnnotation,
        KonanProtoBuf.typeParameterAnnotation
)
