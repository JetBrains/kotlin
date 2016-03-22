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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class CustomBytecodeTextTest : AbstractBytecodeTextTest() {
    fun testEnumMapping() {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL)
        myFiles = CodegenTestFiles.create("whenMappingOrder.kt", """
        enum class MyEnum {
            ENTRY1, ENTRY2, ENTRY3, ENTRY4
        }

        fun f(e: MyEnum) {
            when (e) {
                MyEnum.ENTRY4 -> {}
                MyEnum.ENTRY3 -> {}
                MyEnum.ENTRY2 -> {}
                MyEnum.ENTRY1 -> {}
            }
        }
        """, myEnvironment.project)

        val text = generateToText()
        val getstatics = text.lines().filter { it.contains("GETSTATIC MyEnum.") }.map { it.trim() }
        KtUsefulTestCase.assertOrderedEquals("actual bytecode:\n" + text, listOf(
                "GETSTATIC MyEnum.${'$'}VALUES : [LMyEnum;",
                "GETSTATIC MyEnum.ENTRY4 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY3 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY2 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY1 : LMyEnum;"
        ), getstatics)
    }
}