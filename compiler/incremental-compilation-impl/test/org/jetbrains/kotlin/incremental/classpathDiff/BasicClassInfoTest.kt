/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class BasicClassInfoTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `compute BasicClassInfo`() {
        val compiledClasses = compileJava(className, sourceCode)
        val classIds = compiledClasses.map { BasicClassInfo.compute(it).classId }

        assertEquals(
            listOf(
                classId("com.example", "TopLevelClass", local = false),
                classId("com.example", "TopLevelClass$1", local = true),
                classId("com.example", "TopLevelClass$1LocalClass", local = true),
                classId("com.example", "TopLevelClass$1LocalClass$1LocalClassWithinLocalClass", local = true),
                classId("com.example", "TopLevelClass$1LocalClass.InnerClassWithinLocalClass", local = true),
                classId("com.example", "TopLevelClass$2", local = true),
                classId("com.example", "TopLevelClass.InnerClass", local = false),
                classId("com.example", "TopLevelClass\$InnerClass\$1LocalClassWithinInnerClass", local = true),
                classId("com.example", "TopLevelClass.InnerClass.InnerClassWithinInnerClass", local = false),
                classId("com.example", "TopLevelClass.InnerClassWith\$Sign", local = false),
                classId("com.example", "TopLevelClass.InnerClassWith\$Sign.InnerClassWith\$SignLevel2", local = false),
                classId("com.example", "TopLevelClass.StaticNestedClass", local = false)
            ),
            classIds
        )
    }

    @Suppress("SameParameterValue")
    private fun compileJava(className: String, sourceCode: String): List<ByteArray> {
        val sourceFile = File(tmpDir.newFolder(), "$className.java").apply {
            parentFile.mkdirs()
            writeText(sourceCode)
        }
        val classesDir = tmpDir.newFolder()

        KotlinTestUtils.compileJavaFiles(listOf(sourceFile), listOf("-d", classesDir.path))

        return classesDir.walk().toList()
            .filter { it.isFile }
            .sortedBy { it.path.substringBefore(".class") }
            .map { it.readBytes() }
    }

    private fun classId(@Suppress("SameParameterValue") packageFqName: String, relativeClassName: String, local: Boolean) =
        ClassId(FqName(packageFqName), FqName(relativeClassName), isLocal = local)
}

private const val className = "com/example/TopLevelClass"
private val sourceCode = """
package com.example;

public class TopLevelClass {

    public class InnerClass {
        public class InnerClassWithinInnerClass {
        }
        public void someMethod() {
            class LocalClassWithinInnerClass {
            }
        }
    }

    public static class StaticNestedClass {
    }

    public void methodWithinTopLevelClass() {
        class LocalClass {
            class InnerClassWithinLocalClass {
            }
            public void methodWithinLocalClass() {
                class LocalClassWithinLocalClass {
                }
            }
        }
        Runnable anonymousLocalClassInstance = new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    private Runnable anonymousNonLocalClassInstance = new Runnable() {
        @Override
        public void run() {
        }
    };

    public class InnerClassWith${'$'}Sign {
        public class InnerClassWith${'$'}SignLevel2 {
        }
    }
}
""".trimIndent()
