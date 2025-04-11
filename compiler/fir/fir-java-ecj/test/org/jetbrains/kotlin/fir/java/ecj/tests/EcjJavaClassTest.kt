/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
import org.jetbrains.kotlin.fir.java.ecj.EcjJavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EcjJavaClassTest {

    /**
     * Helper function to check if all expected declarations are present in a Java class.
     *
     * @param javaCode The Java source code as a string.
     * @param expectedDeclarations The list of declaration names that should be present in the compiler class.
     * @param packageName The package name of the Java class.
     * @param className The name of the Java class.
     */
    private fun checkDeclarations(
        javaCode: String,
        expectedDeclarations: List<String>,
        packageName: String = "test",
        className: String = "Test"
    ) {
        // Create a temporary file with the Java source code
        val tempFile = Files.createTempFile("test", ".java").toFile()
        try {
            tempFile.writeText(javaCode)

            // Create an EcjJavaClassFinder with the temporary file
            val finder = EcjJavaClassFinder(listOf(tempFile))

            // Find the class
            val classId = ClassId(FqName(packageName), FqName(className), false)
            val ecjJavaClass = finder.findClass(classId)
            assertNotNull(ecjJavaClass, "Class not found: $classId")

            // Process API declarations and collect their names
            val foundDeclarations = mutableListOf<String>()

            // Add the class itself to the list of declarations
            foundDeclarations.add(className)

            ecjJavaClass.processApiDeclarations { declaration ->
                val name = when (declaration) {
                    is TypeDeclaration -> declaration.name
                    is MethodDeclaration -> declaration.selector
                    is FieldDeclaration -> declaration.name
                    else -> charArrayOf()
                }
                val nameStr = String(name)
                foundDeclarations.add(nameStr)
                declaration
            }

            // Check that all expected declarations are present
            for (expectedDeclaration in expectedDeclarations) {
                assertTrue(
                    foundDeclarations.contains(expectedDeclaration),
                    "Expected declaration not found: $expectedDeclaration. Found declarations: $foundDeclarations",
                )
            }

            // Check that no unexpected declarations are present
            assertEquals(
                expectedDeclarations.size,
                foundDeclarations.size,
                "Found unexpected declarations",
            )
            for (foundDeclaration in foundDeclarations) {
                assertTrue(
                    expectedDeclarations.contains(foundDeclaration),
                    "Unexpected declaration found: $foundDeclaration",
                )
            }
        } finally {
            // Clean up the temporary file
            tempFile.delete()
        }
    }

    @Test
    fun testSimpleClass() {
        val javaCode = """
            package test;

            public class Test {
                public void publicMethod() {}
                private void privateMethod() {}
                public int publicField;
                private int privateField;
            }
        """.trimIndent()

        // Only public methods and fields should be included in API declarations
        checkDeclarations(javaCode, listOf("Test", "publicMethod", "publicField"))
    }

    @Test
    fun testInterface() {
        val javaCode = """
            package test;

            public interface Test {
                void method1();
                void method2();
                int CONSTANT = 42;
            }
        """.trimIndent()

        // All interface members are public by default
        checkDeclarations(javaCode, listOf("Test", "method1", "method2", "CONSTANT"))
    }

    @Test
    fun testNestedClasses() {
        val javaCode = """
            package test;

            public class Test {
                public class PublicNested {
                    public void nestedMethod() {}
                }

                private class PrivateNested {
                    public void nestedMethod() {}
                }

                protected class ProtectedNested {
                    public void nestedMethod() {}
                }
            }
        """.trimIndent()

        // Only public and protected nested classes should be included in API declarations
        checkDeclarations(javaCode, listOf("Test", "PublicNested", "ProtectedNested"))

        // Check the public nested class
        val nestedClassId = ClassId(FqName("test"), FqName("Test.PublicNested"), false)
        checkNestedClass(javaCode, nestedClassId, listOf("PublicNested", "nestedMethod"))

        // Check the protected nested class
        val protectedNestedClassId = ClassId(FqName("test"), FqName("Test.ProtectedNested"), false)
        checkNestedClass(javaCode, protectedNestedClassId, listOf("ProtectedNested", "nestedMethod"))
    }

    private fun checkNestedClass(javaCode: String, nestedClassId: ClassId, expectedDeclarations: List<String>) {
        val tempFile = Files.createTempFile("test", ".java").toFile()
        try {
            tempFile.writeText(javaCode)
            val finder = EcjJavaClassFinder(listOf(tempFile))
            val ecjJavaClass = finder.findClass(nestedClassId)
            assertNotNull(ecjJavaClass, "Nested class not found: $nestedClassId")

            val foundDeclarations = mutableListOf<String>()

            // Add the nested class itself to the list of declarations
            foundDeclarations.add(nestedClassId.shortClassName.asString())

            ecjJavaClass.processApiDeclarations { declaration ->
                val name = when (declaration) {
                    is TypeDeclaration -> declaration.name
                    is MethodDeclaration -> declaration.selector
                    is FieldDeclaration -> declaration.name
                    else -> charArrayOf()
                }
                foundDeclarations.add(String(name))
                declaration
            }

            for (expectedDeclaration in expectedDeclarations) {
                assertTrue(
                    foundDeclarations.contains(expectedDeclaration),
                    "Expected declaration not found in nested class: $expectedDeclaration. Found declarations: $foundDeclarations",
                )
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testClassWithAllFeatures() {
        val javaCode = """
            package test;

            public class Test {
                public int publicField;
                private int privateField;
                protected int protectedField;

                public void publicMethod() {}
                private void privateMethod() {}
                protected void protectedMethod() {}

                public class PublicNested {
                    public void nestedPublicMethod() {}
                    private void nestedPrivateMethod() {}
                }

                private class PrivateNested {}

                protected interface ProtectedInterface {
                    void interfaceMethod();
                }
            }
        """.trimIndent()

        // Check the main class
        checkDeclarations(
            javaCode,
            listOf("Test", "publicField", "protectedField", "publicMethod", "protectedMethod", "PublicNested", "ProtectedInterface")
        )

        // Check the public nested class
        val nestedClassId = ClassId(FqName("test"), FqName("Test.PublicNested"), false)
        checkNestedClass(javaCode, nestedClassId, listOf("PublicNested", "nestedPublicMethod"))

        // Check the protected interface
        val interfaceClassId = ClassId(FqName("test"), FqName("Test.ProtectedInterface"), false)
        checkNestedClass(javaCode, interfaceClassId, listOf("ProtectedInterface", "interfaceMethod"))
    }
}
