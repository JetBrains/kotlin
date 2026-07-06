/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.*
import kotlin.io.path.Path

class SerializabilityTest : BaseCompilationTest() {

    @Test
    fun testSerializability() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = toolchains.jvm.jvmCompilationOperation(emptyList(), Path(".")) {
            compilerArguments[JvmCompilerArguments.CLASSPATH] = listOf(Path("abc.jar"))
        }
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(operation) }
        val operation2: JvmCompilationOperation =
            BtaObjectInputStream(baos.toByteArray().inputStream()).use { it.readObject() as JvmCompilationOperation }
        assertEquals(listOf(Path("abc.jar")), operation2.compilerArguments[JvmCompilerArguments.CLASSPATH]?.joinToString())

    }

    class BtaObjectInputStream(inputStream: InputStream) : ObjectInputStream(inputStream) {
        override fun resolveClass(desc: ObjectStreamClass): Class<*>? {
            return try {
                btaClassloader.loadClass(desc.name)
            } catch (e: ClassNotFoundException) {
                super.resolveClass(desc)
            }
        }
    }
}
