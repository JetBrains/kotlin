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

package org.jetbrains.kotlin.generators.protobuf

import junit.framework.TestCase
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import java.lang.reflect.ParameterizedType
import com.google.protobuf.Descriptors
import kotlin.test.fail
import com.google.common.collect.LinkedHashMultimap
import java.lang.reflect.Modifier

public class ProtoBufConsistencyTest : TestCase() {
    public fun testExtensionNumbersDoNotIntersect() {
        @data class Key(val messageType: Class<*>, val index: Int)

        val extensions = LinkedHashMultimap.create<Key, Descriptors.FieldDescriptor>()

        for (protoPath in PROTO_PATHS) {
            val classFqName = protoPath.packageName + "." + protoPath.debugClassName
            val klass = javaClass.getClassLoader().loadClass(classFqName) ?: error("Class not found: $classFqName")
            for (field in klass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == javaClass<GeneratedExtension<*, *>>()) {
                    // The only place where type information for an extension is stored is the field's declared generic type.
                    // The message type which this extension extends is the first argument to GeneratedExtension<*, *>
                    val containingType = (field.getGenericType() as ParameterizedType).getActualTypeArguments().first() as Class<*>
                    val value = field.get(null) as GeneratedExtension<*, *>
                    val desc = value.getDescriptor()
                    extensions.put(Key(containingType, desc.getNumber()), desc)
                }
            }
        }

        for ((key, descriptors) in extensions.asMap().entrySet()) {
            if (descriptors.size() > 1) {
                fail("""
Several extensions to the same message type with the same index were found.
This will cause different hard-to-debug problems if these extensions are used at the same time during (de-)serialization of the message.
Consider changing the indices in the corresponding .proto definition files.
Message type: ${key.messageType.getSimpleName()}
Index: ${key.index}
Extensions found: ${descriptors.map { it.getName() }}
"""
                )
            }
        }
    }
}
