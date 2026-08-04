/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class JavaParsingTypeSystemTest : JavaParsingTestBase() {

    @Test
    fun testTypeNameStripsTypeArguments() {
        val source = """
            import java.util.List;
            class A {
                List<String> a;
                java.util.Map<String, Integer> b;
                Object[] c;
            }
        """.trimIndent()

        val javaClass = parseFirstClass(source)

        val fieldA = javaClass.fields.first { it.name.asString() == "a" }
        val typeA = fieldA.type as JavaClassifierType
        assertEquals("List", typeA.classifierQualifiedName, "Type name of List<String> must be stripped of type arguments")

        val fieldB = javaClass.fields.first { it.name.asString() == "b" }
        val typeB = fieldB.type as JavaClassifierType
        assertEquals(
            "java.util.Map",
            typeB.classifierQualifiedName,
            "Type name of java.util.Map<String, Integer> must keep the qualifier and drop type arguments",
        )

        val fieldC = javaClass.fields.first { it.name.asString() == "c" }
        val typeC = fieldC.type as JavaArrayType
        val componentType = typeC.componentType as JavaClassifierType
        assertEquals("Object", componentType.classifierQualifiedName)
    }

    @Test
    fun testSimpleTypeArguments() {
        val source = """
            import java.util.List;
            import java.util.Map;
            
            public class MyClass {
                public List<String> items;
                public List<Object> objects;
                public Map<String, Integer> map;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val items = javaClass.fields.first { it.name.asString() == "items" }
        val itemsType = items.type as JavaClassifierType
        assertEquals("List", itemsType.classifierQualifiedName)
        assertEquals(1, itemsType.typeArguments.size)
        val stringArg = itemsType.typeArguments[0] as JavaClassifierType
        assertEquals("String", stringArg.classifierQualifiedName)
        assertNull(stringArg.classifier, "String should have null classifier (needs FIR)")

        val objects = javaClass.fields.first { it.name.asString() == "objects" }
        val objectsType = objects.type as JavaClassifierType
        assertEquals(1, objectsType.typeArguments.size)
        val objectArg = objectsType.typeArguments[0] as JavaClassifierType
        assertEquals("Object", objectArg.classifierQualifiedName)
        assertNull(objectArg.classifier, "Object should have null classifier (needs FIR)")

        val map = javaClass.fields.first { it.name.asString() == "map" }
        val mapType = map.type as JavaClassifierType
        assertEquals("Map", mapType.classifierQualifiedName)
        assertEquals(2, mapType.typeArguments.size)
        val keyArg = mapType.typeArguments[0] as JavaClassifierType
        assertEquals("String", keyArg.classifierQualifiedName)
        val valueArg = mapType.typeArguments[1] as JavaClassifierType
        assertEquals("Integer", valueArg.classifierQualifiedName)
    }

    @Test
    fun testCovariantWildcardReturnType() {
        // Test for the inheritanceWithWildcard pattern:
        // Interface A with method returning X<? extends A>
        // Interface B extends A with covariant override returning Y<? extends B>
        val source = """
            interface A {
                X<? extends A> foo();
                interface X<T extends A> {}
            }
            
            interface B extends A {
                @Override
                Y<? extends B> foo();
                interface Y<U extends B> extends A.X<U> {}
            }
            
            class BImpl implements B {
                @Override
                public B.Y<? extends B> foo() { return null; }
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context

        val classes = tree.getChildren(root).filter { tree.getType(it).toString() == "CLASS" }
        assertEquals(3, classes.size, "Expected 3 classes (A, B, BImpl)")

        // Find interface A
        val interfaceANode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "A" }
        val interfaceA = JavaClassOverAst(interfaceANode, tree, context)
        assertTrue(interfaceA.isInterface, "A should be an interface")

        // Check A.foo() return type
        val aFoo = interfaceA.methods.first { it.name.asString() == "foo" }
        val aFooReturnType = aFoo.returnType as JavaClassifierType
        assertEquals("A.X", aFooReturnType.classifierQualifiedName, "A.foo() should return A.X")
        assertEquals(1, aFooReturnType.typeArguments.size, "A.X should have 1 type argument")

        // Check the wildcard type argument
        val aWildcard = aFooReturnType.typeArguments[0]
        assertTrue(aWildcard is JavaWildcardType, "Expected JavaWildcardType, got ${aWildcard?.javaClass}")
        val aWildcardType = aWildcard as JavaWildcardType
        assertTrue(aWildcardType.isExtends, "Should be '? extends'")
        assertNotNull(aWildcardType.bound) { "Wildcard should have a bound" }
        val aBound = aWildcardType.bound as JavaClassifierType
        assertEquals("A", aBound.classifierQualifiedName, "Wildcard bound should be A")

        // Find interface B
        val interfaceBNode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "B" }
        val interfaceB = JavaClassOverAst(interfaceBNode, tree, context)
        assertTrue(interfaceB.isInterface, "B should be an interface")

        // Check B.foo() return type
        val bFoo = interfaceB.methods.first { it.name.asString() == "foo" }
        val bFooReturnType = bFoo.returnType as JavaClassifierType
        assertEquals("B.Y", bFooReturnType.classifierQualifiedName, "B.foo() should return B.Y")
        assertEquals(1, bFooReturnType.typeArguments.size, "B.Y should have 1 type argument")

        // Check the wildcard type argument
        val bWildcard = bFooReturnType.typeArguments[0]
        assertTrue(bWildcard is JavaWildcardType, "Expected JavaWildcardType, got ${bWildcard?.javaClass}")
        val bWildcardType = bWildcard as JavaWildcardType
        assertTrue(bWildcardType.isExtends, "Should be '? extends'")
        assertNotNull(bWildcardType.bound) { "Wildcard should have a bound" }
        val bBound = bWildcardType.bound as JavaClassifierType
        assertEquals("B", bBound.classifierQualifiedName, "Wildcard bound should be B")

        // Find class BImpl
        val bImplNode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "BImpl" }
        val bImpl = JavaClassOverAst(bImplNode, tree, context)
        assertFalse(bImpl.isInterface, "BImpl should be a class")

        // Check BImpl.foo() return type
        val bImplFoo = bImpl.methods.first { it.name.asString() == "foo" }
        val bImplFooReturnType = bImplFoo.returnType as JavaClassifierType
        assertEquals("B.Y", bImplFooReturnType.classifierQualifiedName, "BImpl.foo() should return B.Y")
        assertEquals(1, bImplFooReturnType.typeArguments.size, "B.Y should have 1 type argument")

        // Check the wildcard type argument
        val bImplWildcard = bImplFooReturnType.typeArguments[0]
        assertTrue(bImplWildcard is JavaWildcardType, "Expected JavaWildcardType, got ${bImplWildcard?.javaClass}")
        val bImplWildcardType = bImplWildcard as JavaWildcardType
        assertTrue(bImplWildcardType.isExtends, "Should be '? extends'")
        assertNotNull(bImplWildcardType.bound) { "Wildcard should have a bound" }
        val bImplBound = bImplWildcardType.bound as JavaClassifierType
        assertEquals("B", bImplBound.classifierQualifiedName, "Wildcard bound should be B")

        // Check that nested interface B.Y properly extends A.X
        val nestedY = interfaceB.findInnerClass(Name.identifier("Y"))
        assertNotNull(nestedY) { "Should find nested interface Y in B" }
        assertTrue(nestedY.isInterface, "Y should be an interface")
        assertEquals(1, nestedY.supertypes.size, "Y should have 1 supertype (A.X)")

        val ySupertype = nestedY.supertypes.first()
        // Y extends A.X<U>, so supertype should be A.X with type argument U
        assertEquals("A.X", ySupertype.classifierQualifiedName, "Y's supertype should be A.X")

        // Check that classifier is resolved for the return types
        // This is important for FIR to properly match method signatures
        assertNotNull(aFooReturnType.classifier) { "A.foo() return type classifier should be resolved" }
        assertEquals(
            interfaceA.findInnerClass(Name.identifier("X")),
            aFooReturnType.classifier,
            "A.foo() return type should resolve to A.X",
        )

        assertNotNull(bFooReturnType.classifier) { "B.foo() return type classifier should be resolved" }
        assertEquals(nestedY, bFooReturnType.classifier, "B.foo() return type should resolve to B.Y")

        assertNotNull(bImplFooReturnType.classifier) { "BImpl.foo() return type classifier should be resolved" }
        assertEquals(nestedY, bImplFooReturnType.classifier, "BImpl.foo() return type should resolve to B.Y")
    }

    @Test
    fun testUnboundedWildcard() {
        // Test unbounded wildcard (?) which should have isExtends=true and bound=null
        val source = """
            import java.util.List;
            
            public class MyClass {
                public List<?> items;
                public List<? extends Object> explicitExtends;
                public List<? super String> superWildcard;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        // Test unbounded wildcard: List<?>
        val itemsField = javaClass.fields.first { it.name.asString() == "items" }
        val itemsType = itemsField.type as JavaClassifierType
        assertEquals(1, itemsType.typeArguments.size, "List should have 1 type argument")

        val unboundedWildcard = itemsType.typeArguments[0]
        assertTrue(unboundedWildcard is JavaWildcardType, "Expected JavaWildcardType for ?, got ${unboundedWildcard?.javaClass}")
        val unboundedType = unboundedWildcard as JavaWildcardType
        assertTrue(unboundedType.isExtends, "Unbounded wildcard should have isExtends=true")
        assertNull(unboundedType.bound, "Unbounded wildcard should have bound=null")

        // Test explicit extends Object: List<? extends Object>
        val extendsField = javaClass.fields.first { it.name.asString() == "explicitExtends" }
        val extendsType = extendsField.type as JavaClassifierType
        val extendsWildcard = extendsType.typeArguments[0] as JavaWildcardType
        assertTrue(extendsWildcard.isExtends, "? extends Object should have isExtends=true")
        assertNotNull(extendsWildcard.bound) { "? extends Object should have a bound" }
        val extendsBound = extendsWildcard.bound as JavaClassifierType
        assertEquals("Object", extendsBound.classifierQualifiedName)

        // Test super wildcard: List<? super String>
        val superField = javaClass.fields.first { it.name.asString() == "superWildcard" }
        val superType = superField.type as JavaClassifierType
        val superWildcard = superType.typeArguments[0] as JavaWildcardType
        assertFalse(superWildcard.isExtends, "? super String should have isExtends=false")
        assertNotNull(superWildcard.bound) { "? super String should have a bound" }
        val superBound = superWildcard.bound as JavaClassifierType
        assertEquals("String", superBound.classifierQualifiedName)
    }

    @Test
    fun testRawTypeDetection() {
        // Test raw types - generic class used without type arguments
        val source = """
            public class Generic<T> {
                public static Generic raw = new Generic();
                public Generic<String> notRaw = new Generic<>();
                public Generic alsoRaw;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.typeParameters.size, "Generic should have 1 type parameter")
        assertEquals("T", javaClass.typeParameters.first().name.asString())

        // Check the static raw field
        val rawField = javaClass.fields.first { it.name.asString() == "raw" }
        val rawType = rawField.type as JavaClassifierType
        assertEquals("Generic", rawType.classifierQualifiedName)
        assertTrue(rawType.typeArguments.isEmpty(), "Raw type should have no type arguments")
        // classifier should resolve to the containing class itself
        assertNotNull(rawType.classifier) { "classifier should resolve to Generic class" }
        assertEquals(javaClass, rawType.classifier, "classifier should be the same Generic class")
        // isRaw should be true because Generic has type params but no args provided
        assertTrue(rawType.isRaw, "Expected isRaw=true for raw Generic field")

        // Check the notRaw field (has explicit type argument)
        val notRawField = javaClass.fields.first { it.name.asString() == "notRaw" }
        val notRawType = notRawField.type as JavaClassifierType
        assertEquals("Generic", notRawType.classifierQualifiedName)
        assertEquals(1, notRawType.typeArguments.size, "notRaw should have 1 type argument")
        assertFalse(notRawType.isRaw, "Expected isRaw=false for Generic<String>")

        // Check the alsoRaw field (instance field, also raw)
        val alsoRawField = javaClass.fields.first { it.name.asString() == "alsoRaw" }
        val alsoRawType = alsoRawField.type as JavaClassifierType
        assertTrue(alsoRawType.typeArguments.isEmpty(), "alsoRaw should have no type arguments")
        assertTrue(alsoRawType.isRaw, "Expected isRaw=true for raw alsoRaw field")
    }

    @Test
    fun testRawTypeWithExternalClass() {
        // Test raw types with external classes (via star import)
        val source = """
            import java.util.*;
            public class A {
                void foo(List x) {}
                void bar(List<String> y) {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val fooMethod = javaClass.methods.first { it.name.asString() == "foo" }
        val fooParamType = fooMethod.valueParameters.first().type as JavaClassifierType
        // For external class via star import, classifier is null (not in local scope)
        assertNull(fooParamType.classifier, "External class List should have null classifier")
        // classifierQualifiedName returns "List" (unresolved via star import)
        assertEquals("List", fooParamType.classifierQualifiedName)
        assertTrue(fooParamType.typeArguments.isEmpty(), "Raw List should have no type args")
        // isRaw returns false for external classes because we can't determine type params without FIR
        // FIR's type conversion handles this via fallback logic
        // This documents the current behavior - java-direct can't determine isRaw for external classes
        assertFalse(fooParamType.isRaw, "isRaw is false for external classes (FIR handles this)")

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barParamType = barMethod.valueParameters.first().type as JavaClassifierType
        assertEquals(1, barParamType.typeArguments.size, "List<String> should have 1 type arg")
        assertFalse(barParamType.isRaw, "List<String> should not be raw")
    }
}
