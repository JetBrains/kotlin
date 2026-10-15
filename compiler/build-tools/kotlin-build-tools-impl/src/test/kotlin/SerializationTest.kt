@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SerializationTest {

    interface Serializer {
        fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T

        object Protobuf : Serializer {
            override fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T {
                val byteArray = ProtoBuf.encodeToByteArray(serializer(clazz.java), value)
                @Suppress("UNCHECKED_CAST")
                return ProtoBuf.decodeFromByteArray(serializer(clazz.java), byteArray) as T
            }
        }
    }

    inline fun <reified T : Any> Serializer.serializeAndDeserialize(value: T): T = serializeAndDeserialize(value, T::class)

    val serializer = Serializer.Protobuf

    @Test
    @DisplayName("Argument classes are serialized correctly")
    fun argumentClasses() {
        val jvmArguments = JvmCompilerArgumentsImpl()
        jvmArguments.`jdk-home` = Path("/someBuildFile.xml")
        jvmArguments.`jvm-default` = JvmDefaultMode.NO_COMPATIBILITY
        val jvmArguments2 = serializer.serializeAndDeserialize<JvmCompilerArgumentsImpl>(jvmArguments)
        assertEquals(jvmArguments.`jdk-home`, jvmArguments2.`jdk-home`)
        assertEquals(jvmArguments.`jvm-default`, jvmArguments2.`jvm-default`)
        assertEquals(jvmArguments.toArgumentStrings(), jvmArguments2.toArgumentStrings())
    }
}
