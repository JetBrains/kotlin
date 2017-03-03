/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.*

internal class KonanSerializerExtension(val context: Context) :
        KotlinSerializerExtensionBase(KonanSerializerProtocol) {

    override val stringTable = KonanStringTable()

    private val backingFieldClass =
        context.builtIns.getKonanInternalClass("HasBackingField").getDefaultType()

    private val backingFieldAnnotation = AnnotationDescriptorImpl(
       backingFieldClass, emptyMap(), SourceElement.NO_SOURCE)

    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        super.serializeProperty(descriptor, proto)

        if (context.ir.propertiesWithBackingFields.contains(descriptor)) {
            proto.addExtension(KonanLinkData.propertyAnnotation,
                annotationSerializer.serializeAnnotation(backingFieldAnnotation))

            proto.flags = proto.flags or Flags.HAS_ANNOTATIONS.toFlags(true)
        }
    }
}

object KonanSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply {
           KonanLinkData.registerAllExtensions(this)
        },
        KonanLinkData.packageFqName,
        KonanLinkData.constructorAnnotation,
        KonanLinkData.classAnnotation,
        KonanLinkData.functionAnnotation,
        KonanLinkData.propertyAnnotation,
        KonanLinkData.enumEntryAnnotation,
        KonanLinkData.compileTimeValue,
        KonanLinkData.parameterAnnotation,
        KonanLinkData.typeAnnotation,
        KonanLinkData.typeParameterAnnotation
)
