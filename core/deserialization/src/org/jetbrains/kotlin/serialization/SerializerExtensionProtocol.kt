/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.GeneratedExtension

open class SerializerExtensionProtocol(
        val extensionRegistry: ExtensionRegistryLite,
        val packageFqName: GeneratedExtension<ProtoBuf.Package, Int>,
        val constructorAnnotation: GeneratedExtension<ProtoBuf.Constructor, List<ProtoBuf.Annotation>>,
        val classAnnotation: GeneratedExtension<ProtoBuf.Class, List<ProtoBuf.Annotation>>,
        val functionAnnotation: GeneratedExtension<ProtoBuf.Function, List<ProtoBuf.Annotation>>,
        val propertyAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
        val enumEntryAnnotation: GeneratedExtension<ProtoBuf.EnumEntry, List<ProtoBuf.Annotation>>,
        val compileTimeValue: GeneratedExtension<ProtoBuf.Property, ProtoBuf.Annotation.Argument.Value>,
        val parameterAnnotation: GeneratedExtension<ProtoBuf.ValueParameter, List<ProtoBuf.Annotation>>,
        val typeAnnotation: GeneratedExtension<ProtoBuf.Type, List<ProtoBuf.Annotation>>,
        val typeParameterAnnotation: GeneratedExtension<ProtoBuf.TypeParameter, List<ProtoBuf.Annotation>>
)
