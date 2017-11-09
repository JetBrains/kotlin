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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType

abstract class SerializerExtension {
    abstract val stringTable: StringTable

    val annotationSerializer by lazy { AnnotationSerializer(stringTable) }

    open fun shouldUseTypeTable(): Boolean = false

    open fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {
    }

    open fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
    }

    open fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder) {
    }

    open fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) {
    }

    open fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
    }

    open fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
    }

    open fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
    }

    open fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
    }

    open fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
    }

    open fun serializeErrorType(type: KotlinType, builder: ProtoBuf.Type.Builder) {
        throw IllegalStateException("Cannot serialize error type: $type")
    }
}
