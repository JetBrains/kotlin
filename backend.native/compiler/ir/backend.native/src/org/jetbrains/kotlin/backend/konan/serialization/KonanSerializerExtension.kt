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
import org.jetbrains.kotlin.backend.konan.getKonanInternalClass
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.FlexibleType

internal class KonanSerializerExtension(val context: Context) :
        KotlinSerializerExtensionBase(KonanSerializerProtocol) {

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
