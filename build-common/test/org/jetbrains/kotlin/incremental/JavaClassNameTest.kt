/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class JavaClassNameTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `test compute JavaClassName`() {
        val compiledClasses = compileJava(className, sourceCode)
        val classNames = compiledClasses.map { JavaClassName.compute(it) }

        assertEquals(
            listOf(
                "TopLevelClass: com/example/JavaClassWithNestedClasses",
                "LocalClass: com/example/JavaClassWithNestedClasses\$1",
                "LocalClass: com/example/JavaClassWithNestedClasses\$1LocalClass",
                "NestedNonLocalClass: com/example/JavaClassWithNestedClasses\$1LocalClass\$InnerClassWithinLocalClass",
                "LocalClass: com/example/JavaClassWithNestedClasses\$2",
                "NestedNonLocalClass: com/example/JavaClassWithNestedClasses\$InnerClass",
                "LocalClass: com/example/JavaClassWithNestedClasses\$InnerClass\$1LocalClassWithinInnerClass",
                "NestedNonLocalClass: com/example/JavaClassWithNestedClasses\$InnerClass\$InnerClassWithinInnerClass",
                "NestedNonLocalClass: com/example/JavaClassWithNestedClasses\$InnerClassWith\$Sign",
                "NestedNonLocalClass: com/example/JavaClassWithNestedClasses\$StaticNestedClass"
            ),
            classNames.map { "${it.javaClass.simpleName}: ${it.name}" }
        )
    }

    @Test
    fun `test computeJavaClassIds`() {
        val compiledClasses = compileJava(className, sourceCode)
        val classNames = compiledClasses.map { JavaClassName.compute(it) }
        val classIds = computeJavaClassIds(classNames)

        assertEquals(
            listOf(
                classId("com.example", "JavaClassWithNestedClasses", local = false),
                classId("com.example", "JavaClassWithNestedClasses$1", local = true),
                classId("com.example", "JavaClassWithNestedClasses$1LocalClass", local = true),
                classId("com.example", "JavaClassWithNestedClasses$1LocalClass.InnerClassWithinLocalClass", local = true),
                classId("com.example", "JavaClassWithNestedClasses$2", local = true),
                classId("com.example", "JavaClassWithNestedClasses.InnerClass", local = false),
                classId("com.example", "JavaClassWithNestedClasses\$InnerClass\$1LocalClassWithinInnerClass", local = true),
                classId("com.example", "JavaClassWithNestedClasses.InnerClass.InnerClassWithinInnerClass", local = false),
                classId("com.example", "JavaClassWithNestedClasses.InnerClassWith\$Sign", local = false),
                classId("com.example", "JavaClassWithNestedClasses.StaticNestedClass", local = false)
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

        return classesDir.walk().filter { it.isFile }
            .sortedBy { it.path.substringBefore(".class") }
            .map { it.readBytes() }
            .toList()
    }

    private fun classId(@Suppress("SameParameterValue") packageFqName: String, relativeClassName: String, local: Boolean) =
        ClassId(FqName(packageFqName), FqName(relativeClassName), local)
}

private const val className = "com/example/JavaClassWithNestedClasses"
private val sourceCode = """
package com.example;

public class JavaClassWithNestedClasses {

    public class InnerClass {

        public void publicMethod() {
            System.out.println("I'm in a public method");
        }

        private void privateMethod() {
            System.out.println("I'm in a private method");
        }

        public class InnerClassWithinInnerClass {
        }

        public void someMethod() {

            class LocalClassWithinInnerClass {
            }
        }
    }

    public static class StaticNestedClass {
    }

    public void someMethod() {

        class LocalClass {

            class InnerClassWithinLocalClass {
            }
        }

        Runnable objectOfAnonymousLocalClass = new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    private Runnable objectOfAnonymousNonLocalClass = new Runnable() {
        @Override
        public void run() {
        }
    };

    public class InnerClassWith${'$'}Sign {
    }
}
""".trimIndent()
