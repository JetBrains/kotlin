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
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.junit.Assert.assertNotEquals
import org.junit.Test

class JvmBuildMetaInfoTest : TestCase() {
    @Test
    fun testSerialization() {
        val args = K2JVMCompilerArguments()
        val info = JvmBuildMetaInfo(args)
        val actual = JvmBuildMetaInfo.serializeToString(info)
        val expectedTempalte =
"""apiVersionString=1.1
bytecodeVersionMajor=1
bytecodeVersionMinor=0
bytecodeVersionPatch=1
compilerBuildVersion=@snapshot@
coroutinesEnable=false
coroutinesError=false
coroutinesVersion=0
coroutinesWarn=false
isEAP=@isEAP@
languageVersionString=1.1
metadataVersionMajor=1
metadataVersionMinor=1
metadataVersionPatch=3
multiplatformEnable=false
multiplatformVersion=0
ownVersion=0"""
        val expected = expectedTempalte.replace("@snapshot@", KotlinCompilerVersion.VERSION)
                                       .replace("@isEAP@", KotlinCompilerVersion.IS_PRE_RELEASE.toString())
        assertEquals(expected, actual)
    }

    @Test
    fun testSerializationDeserialization() {
        val args = K2JVMCompilerArguments()
        val info = JvmBuildMetaInfo(args)
        val serialized = JvmBuildMetaInfo.serializeToString(info)
        val deserialized = JvmBuildMetaInfo.deserializeFromString(serialized)
        assertEquals(info, deserialized)
    }

    @Test
    fun testEquals() {
        val args1 = K2JVMCompilerArguments()
        args1.coroutinesEnable = true
        val info1 = JvmBuildMetaInfo(args1)

        val args2 = K2JVMCompilerArguments()
        args2.coroutinesEnable = false
        val info2 = JvmBuildMetaInfo(args2)

        assertNotEquals(info1, info2)
        assertEquals(info1, info2.copy(coroutinesEnable = true))
    }
}