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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package kotlin.reflect.jvm

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfoTable
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.EmptyContainerForLocal
import kotlin.reflect.jvm.internal.KFunctionImpl
import kotlin.reflect.jvm.internal.getOrCreateModule

/**
 * This is an experimental API. Given a class for a compiled Kotlin lambda or a function expression,
 * returns a [KFunction] instance providing introspection capabilities for that lambda or function expression and its parameters.
 * Not all features are currently supported, in particular [KCallable.call] and [KCallable.callBy] will fail at the moment.
 */
fun <R> Function<R>.reflect(): KFunction<R>? {
    val annotation = javaClass.getAnnotation(Metadata::class.java) ?: return null
    val data = annotation.d1.takeUnless(Array<String>::isEmpty) ?: return null
    val input = BitEncoding.decodeBytes(data).inputStream()
    val stringTableTypes = JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, JvmProtoBufUtil.EXTENSION_REGISTRY)
    val nameResolver = JvmNameResolver(stringTableTypes, annotation.d2)
    val proto = ProtoBuf.Function.parseFrom(input, JvmProtoBufUtil.EXTENSION_REGISTRY)

    val descriptor = deserializeToDescriptor(javaClass, proto, nameResolver, TypeTable(proto.typeTable), MemberDeserializer::loadFunction)
                     ?: return null

    @Suppress("UNCHECKED_CAST")
    return KFunctionImpl(EmptyContainerForLocal, descriptor) as KFunction<R>
}

internal fun <M : MessageLite, D : CallableDescriptor> deserializeToDescriptor(
        moduleAnchor: Class<*>,
        proto: M,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        createDescriptor: MemberDeserializer.(M) -> D
): D? {
    val moduleData = moduleAnchor.getOrCreateModule()

    val typeParameters = when (proto) {
        is ProtoBuf.Function -> proto.typeParameterList
        is ProtoBuf.Property -> proto.typeParameterList
        else -> error("Unsupported message: $proto")
    }

    val context = DeserializationContext(
            moduleData.deserialization, nameResolver, moduleData.module, typeTable, SinceKotlinInfoTable.EMPTY,
            containerSource = null, parentTypeDeserializer = null, typeParameters = typeParameters
    )
    return MemberDeserializer(context).createDescriptor(proto)
}
