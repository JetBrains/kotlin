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

package kotlin.reflect.jvm

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import kotlin.jvm.internal.KotlinCallable
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.EmptyContainerForLocal
import kotlin.reflect.jvm.internal.KFunctionImpl
import kotlin.reflect.jvm.internal.getOrCreateModule

/**
 * This is an experimental API. Given a class for a compiled Kotlin lambda or a function expression,
 * returns a [KFunction] instance providing introspection capabilities for that lambda or function expression and its parameters.
 * Not all features are currently supported, in particular [KCallable.call] and [KCallable.callBy] will fail at the moment.
 */
public fun <R> Function<R>.reflect(): KFunction<R>? {
    val callable = javaClass.getAnnotation(KotlinCallable::class.java) ?: return null
    val input = BitEncoding.decodeBytes(callable.data).inputStream()
    val nameResolver = JvmNameResolver(
            JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, JvmProtoBufUtil.EXTENSION_REGISTRY),
            callable.strings
    )
    val proto = ProtoBuf.Callable.parseFrom(input, JvmProtoBufUtil.EXTENSION_REGISTRY)
    val moduleData = javaClass.getOrCreateModule()
    val context = DeserializationContext(moduleData.deserialization, nameResolver, moduleData.module,
                                         parentTypeDeserializer = null, typeParameters = listOf())
    val descriptor = MemberDeserializer(context).loadCallable(proto) as FunctionDescriptor
    @Suppress("UNCHECKED_CAST")
    return KFunctionImpl(EmptyContainerForLocal, descriptor) as KFunction<R>
}
