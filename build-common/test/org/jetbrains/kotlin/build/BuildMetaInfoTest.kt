/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.build

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BuildMetaInfoTest : TestCase() {
    @Test
    fun testJvmSerialization() {
        val args = K2JVMCompilerArguments()
        val info = JvmBuildMetaInfo.create(args)
        val actual = JvmBuildMetaInfo.serializeToString(info)
        val expectedKeys = listOf(
            "apiVersionString",
            "bytecodeVersionMajor",
            "bytecodeVersionMinor",
            "bytecodeVersionPatch",
            "compilerBuildVersion",
            "coroutinesEnable",
            "coroutinesError",
            "coroutinesVersion",
            "coroutinesWarn",
            "isEAP",
            "languageVersionString",
            "metadataVersionMajor",
            "metadataVersionMinor",
            "metadataVersionPatch",
            "multiplatformEnable",
            "multiplatformVersion",
            "ownVersion"
        )
        assertEquals(expectedKeys, actual.split("\r\n", "\n").map { line -> line.split("=").first() })
    }

    @Test
    fun testJvmSerializationDeserialization() {
        val args = K2JVMCompilerArguments()
        val info = JvmBuildMetaInfo.create(args)
        val serialized = JvmBuildMetaInfo.serializeToString(info)
        val deserialized = JvmBuildMetaInfo.deserializeFromString(serialized)
        assertEquals(info, deserialized)
    }

    @Test
    fun testJsSerializationDeserialization() {
        val args = K2JVMCompilerArguments()
        val info = JvmBuildMetaInfo.create(args)
        val serialized = JvmBuildMetaInfo.serializeToString(info)
        val deserialized = JvmBuildMetaInfo.deserializeFromString(serialized)
        assertEquals(info, deserialized)
    }

    @Test
    fun testJvmEquals() {
        val args1 = K2JVMCompilerArguments()
        args1.coroutinesState = CommonCompilerArguments.ENABLE
        val info1 = JvmBuildMetaInfo.create(args1)

        val args2 = K2JVMCompilerArguments()
        args2.coroutinesState = CommonCompilerArguments.WARN
        val info2 = JvmBuildMetaInfo.create(args2)

        assertNotEquals(info1, info2)
        assertEquals(info1, info2.copy(coroutinesEnable = true, coroutinesWarn = false))
    }
}
