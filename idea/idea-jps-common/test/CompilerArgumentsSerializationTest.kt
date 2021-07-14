// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.junit.Test
import kotlin.random.Random
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class CompilerArgumentsSerializationTest {

    @Test
    fun testDummyJVM() {
        doSerializeDeserializeAndCompareTest<K2JVMCompilerArguments>()
    }

    @Test
    fun testRandomFlagArgumentsJVM() {
        doRandomFlagArgumentsTest<K2JVMCompilerArguments>()
    }

    @Test
    fun testRandomStringArgumentsJVM() {
        doRandomStringArgumentsTest<K2JVMCompilerArguments>()
    }

    @Test
    fun testLongClasspathArgumentJVM() {
        doSerializeDeserializeAndCompareTest<K2JVMCompilerArguments> {
            classpath = generateSequence { generateRandomString(50) }.take(10).toList().joinToString(File.pathSeparator)
        }
    }


    @Test
    fun testRandomArrayArgumentsJVM() {
        doRandomArrayArgumentsTest<K2JVMCompilerArguments>()
    }

    @Test
    fun testDummyJs() {
        doSerializeDeserializeAndCompareTest<K2JSCompilerArguments>()
    }

    @Test
    fun testRandomFlagArgumentsJS() {
        doRandomFlagArgumentsTest<K2JSCompilerArguments>()
    }

    @Test
    fun testRandomStringArgumentsJS() {
        doRandomStringArgumentsTest<K2JSCompilerArguments>()
    }

    @Test
    fun testRandomArrayArgumentsJS() {
        doRandomArrayArgumentsTest<K2JSCompilerArguments>()
    }

    @Test
    fun testDummyMetadata() {
        doSerializeDeserializeAndCompareTest<K2MetadataCompilerArguments>()
    }

    @Test
    fun testRandomFlagArgumentsMetadata() {
        doRandomFlagArgumentsTest<K2MetadataCompilerArguments>()
    }

    @Test
    fun testRandomStringArgumentsMetadata() {
        doRandomStringArgumentsTest<K2MetadataCompilerArguments>()
    }

    @Test
    fun testLongClasspathArgumentMetadata() {
        doSerializeDeserializeAndCompareTest<K2MetadataCompilerArguments> {
            classpath = generateSequence { generateRandomString(50) }.take(10).toList().joinToString(File.pathSeparator)
        }
    }

    @Test
    fun testRandomArrayArgumentsMetadata() {
        doRandomArrayArgumentsTest<K2MetadataCompilerArguments>()
    }


    @Test
    fun testDummyJsDce() {
        doSerializeDeserializeAndCompareTest<K2JSDceArguments>()
    }

    @Test
    fun testRandomFlagArgumentsJSDce() {
        doRandomFlagArgumentsTest<K2JSDceArguments>()
    }

    @Test
    fun testRandomStringArgumentsJSDce() {
        doRandomStringArgumentsTest<K2JSDceArguments>()
    }

    @Test
    fun testRandomArrayArgumentsJSDce() {
        doRandomArrayArgumentsTest<K2JSDceArguments>()
    }


    private inline fun <reified T : CommonToolArguments> doSerializeDeserializeAndCompareTest(configure: T.() -> Unit = {}) {
        val oldInstance = T::class.java.getConstructor().newInstance().apply(configure)
        val serializer = CompilerArgumentsSerializerV5<T>(oldInstance)
        val mockFacetElement = Element("ROOT")
        val element = serializer.serializeTo(mockFacetElement)
        val newInstance = T::class.java.getConstructor().newInstance()
        val deserializer = CompilerArgumentsDeserializerV5(newInstance)
        deserializer.deserializeFrom(element)
        T::class.memberProperties.mapNotNull { it.safeAs<KProperty1<T, *>>() }.forEach {
            assert(it.get(oldInstance) == it.get(newInstance)) {
                "Property ${it.name} has different values before (${it.get(oldInstance)}) and after (${it.get(newInstance)}) serialization"
            }
        }
    }

    private inline fun <reified T : CommonToolArguments> doRandomFlagArgumentsTest() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(T::class)
        val randomFlags = generateSequence { Random.nextBoolean() }.take(flagProperties.size).toList()
        doSerializeDeserializeAndCompareTest<T> {
            flagProperties.zip(randomFlags).forEach {
                it.first.cast<KMutableProperty1<T, Boolean>>().set(this, it.second)
            }
        }
    }

    private inline fun <reified T : CommonToolArguments> doRandomStringArgumentsTest() {
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(T::class)
        val randomStrings = generateSequence { generateRandomString(Random.nextInt(20)) }.take(stringProperties.size).toList()
        doSerializeDeserializeAndCompareTest<T> {
            stringProperties.zip(randomStrings).forEach {
                it.first.cast<KMutableProperty1<T, String?>>().set(this, it.second)
            }
        }
    }

    private inline fun <reified T : CommonToolArguments> doRandomArrayArgumentsTest() {
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(T::class)
        val randomArrays = generateSequence {
            generateSequence { generateRandomString(Random.nextInt(20)) }.take(Random.nextInt(10)).toList().toTypedArray()
        }.take(arrayProperties.size).toList()
        doSerializeDeserializeAndCompareTest<T> {
            arrayProperties.zip(randomArrays).forEach {
                it.first.cast<KMutableProperty1<T, Array<String>?>>().set(this, it.second)
            }
        }
    }

    companion object {
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        private fun generateRandomString(length: Int) = generateSequence { Random.nextInt(0, charPool.size) }
            .take(length)
            .map(charPool::get)
            .joinToString("")
    }
}