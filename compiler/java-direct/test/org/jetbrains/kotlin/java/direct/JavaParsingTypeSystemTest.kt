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
import org.junit.jupiter.api.Test

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
        assert(typeA.classifierQualifiedName == "List") {
            "Expected simple name List for List<String>, got ${typeA.classifierQualifiedName}"
        }

        val fieldB = javaClass.fields.first { it.name.asString() == "b" }
        val typeB = fieldB.type as JavaClassifierType
        assert(typeB.classifierQualifiedName == "java.util.Map") {
            "Expected qualified name java.util.Map for java.util.Map<String, Integer>, got ${typeB.classifierQualifiedName}"
        }

        val fieldC = javaClass.fields.first { it.name.asString() == "c" }
        val typeC = fieldC.type as JavaArrayType
        val componentType = typeC.componentType as JavaClassifierType
        assert(componentType.classifierQualifiedName == "Object") {
            "Expected component type Object for Object[], got ${componentType.classifierQualifiedName}"
        }
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
        assert(itemsType.classifierQualifiedName == "List") { "Expected 'List', got ${itemsType.classifierQualifiedName}" }
        assert(itemsType.typeArguments.size == 1) { "Expected 1 type argument, got ${itemsType.typeArguments.size}" }
        val stringArg = itemsType.typeArguments[0] as JavaClassifierType
        assert(stringArg.classifierQualifiedName == "String") { "Expected 'String', got ${stringArg.classifierQualifiedName}" }
        assert(stringArg.classifier == null) { "String should have null classifier (needs FIR)" }

        val objects = javaClass.fields.first { it.name.asString() == "objects" }
        val objectsType = objects.type as JavaClassifierType
        assert(objectsType.typeArguments.size == 1) { "Expected 1 type argument, got ${objectsType.typeArguments.size}" }
        val objectArg = objectsType.typeArguments[0] as JavaClassifierType
        assert(objectArg.classifierQualifiedName == "Object") { "Expected 'Object', got ${objectArg.classifierQualifiedName}" }
        assert(objectArg.classifier == null) { "Object should have null classifier (needs FIR)" }

        val map = javaClass.fields.first { it.name.asString() == "map" }
        val mapType = map.type as JavaClassifierType
        assert(mapType.classifierQualifiedName == "Map") { "Expected 'Map', got ${mapType.classifierQualifiedName}" }
        assert(mapType.typeArguments.size == 2) { "Expected 2 type arguments, got ${mapType.typeArguments.size}" }
        val keyArg = mapType.typeArguments[0] as JavaClassifierType
        assert(keyArg.classifierQualifiedName == "String") { "Expected 'String', got ${keyArg.classifierQualifiedName}" }
        val valueArg = mapType.typeArguments[1] as JavaClassifierType
        assert(valueArg.classifierQualifiedName == "Integer") { "Expected 'Integer', got ${valueArg.classifierQualifiedName}" }
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
        assert(classes.size == 3) { "Expected 3 classes (A, B, BImpl), got ${classes.size}" }

        // Find interface A
        val interfaceANode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "A" }
        val interfaceA = JavaClassOverAst(interfaceANode, tree, context)
        assert(interfaceA.isInterface) { "A should be an interface" }

        // Check A.foo() return type
        val aFoo = interfaceA.methods.first { it.name.asString() == "foo" }
        val aFooReturnType = aFoo.returnType as JavaClassifierType
        assert(aFooReturnType.classifierQualifiedName == "A.X") {
            "A.foo() should return A.X, got ${aFooReturnType.classifierQualifiedName}"
        }
        assert(aFooReturnType.typeArguments.size == 1) {
            "A.X should have 1 type argument, got ${aFooReturnType.typeArguments.size}"
        }

        // Check the wildcard type argument
        val aWildcard = aFooReturnType.typeArguments[0]
        assert(aWildcard is JavaWildcardType) {
            "Expected JavaWildcardType, got ${aWildcard?.javaClass}"
        }
        val aWildcardType = aWildcard as JavaWildcardType
        assert(aWildcardType.isExtends) { "Should be '? extends'" }
        assert(aWildcardType.bound != null) { "Wildcard should have a bound" }
        val aBound = aWildcardType.bound as JavaClassifierType
        assert(aBound.classifierQualifiedName == "A") {
            "Wildcard bound should be A, got ${aBound.classifierQualifiedName}"
        }

        // Find interface B
        val interfaceBNode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "B" }
        val interfaceB = JavaClassOverAst(interfaceBNode, tree, context)
        assert(interfaceB.isInterface) { "B should be an interface" }

        // Check B.foo() return type
        val bFoo = interfaceB.methods.first { it.name.asString() == "foo" }
        val bFooReturnType = bFoo.returnType as JavaClassifierType
        assert(bFooReturnType.classifierQualifiedName == "B.Y") {
            "B.foo() should return B.Y, got ${bFooReturnType.classifierQualifiedName}"
        }
        assert(bFooReturnType.typeArguments.size == 1) {
            "B.Y should have 1 type argument, got ${bFooReturnType.typeArguments.size}"
        }

        // Check the wildcard type argument
        val bWildcard = bFooReturnType.typeArguments[0]
        assert(bWildcard is JavaWildcardType) {
            "Expected JavaWildcardType, got ${bWildcard?.javaClass}"
        }
        val bWildcardType = bWildcard as JavaWildcardType
        assert(bWildcardType.isExtends) { "Should be '? extends'" }
        assert(bWildcardType.bound != null) { "Wildcard should have a bound" }
        val bBound = bWildcardType.bound as JavaClassifierType
        assert(bBound.classifierQualifiedName == "B") {
            "Wildcard bound should be B, got ${bBound.classifierQualifiedName}"
        }

        // Find class BImpl
        val bImplNode =
            classes.first { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "BImpl" }
        val bImpl = JavaClassOverAst(bImplNode, tree, context)
        assert(!bImpl.isInterface) { "BImpl should be a class" }

        // Check BImpl.foo() return type
        val bImplFoo = bImpl.methods.first { it.name.asString() == "foo" }
        val bImplFooReturnType = bImplFoo.returnType as JavaClassifierType
        assert(bImplFooReturnType.classifierQualifiedName == "B.Y") {
            "BImpl.foo() should return B.Y, got ${bImplFooReturnType.classifierQualifiedName}"
        }
        assert(bImplFooReturnType.typeArguments.size == 1) {
            "B.Y should have 1 type argument, got ${bImplFooReturnType.typeArguments.size}"
        }

        // Check the wildcard type argument
        val bImplWildcard = bImplFooReturnType.typeArguments[0]
        assert(bImplWildcard is JavaWildcardType) {
            "Expected JavaWildcardType, got ${bImplWildcard?.javaClass}"
        }
        val bImplWildcardType = bImplWildcard as JavaWildcardType
        assert(bImplWildcardType.isExtends) { "Should be '? extends'" }
        assert(bImplWildcardType.bound != null) { "Wildcard should have a bound" }
        val bImplBound = bImplWildcardType.bound as JavaClassifierType
        assert(bImplBound.classifierQualifiedName == "B") {
            "Wildcard bound should be B, got ${bImplBound.classifierQualifiedName}"
        }

        // Check that nested interface B.Y properly extends A.X
        val nestedY = interfaceB.findInnerClass(Name.identifier("Y"))
        assert(nestedY != null) { "Should find nested interface Y in B" }
        assert(nestedY!!.isInterface) { "Y should be an interface" }
        assert(nestedY.supertypes.size == 1) { "Y should have 1 supertype (A.X), got ${nestedY.supertypes.size}" }

        val ySupertype = nestedY.supertypes.first()
        // Y extends A.X<U>, so supertype should be A.X with type argument U
        assert(ySupertype.classifierQualifiedName == "A.X") {
            "Y's supertype should be A.X, got ${ySupertype.classifierQualifiedName}"
        }

        // Check that classifier is resolved for the return types
        // This is important for FIR to properly match method signatures
        assert(aFooReturnType.classifier != null) { "A.foo() return type classifier should be resolved" }
        assert(aFooReturnType.classifier == interfaceA.findInnerClass(Name.identifier("X"))) {
            "A.foo() return type should resolve to A.X"
        }

        assert(bFooReturnType.classifier != null) { "B.foo() return type classifier should be resolved" }
        assert(bFooReturnType.classifier == nestedY) {
            "B.foo() return type should resolve to B.Y"
        }

        assert(bImplFooReturnType.classifier != null) { "BImpl.foo() return type classifier should be resolved" }
        assert(bImplFooReturnType.classifier == nestedY) {
            "BImpl.foo() return type should resolve to B.Y"
        }
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
        assert(itemsType.typeArguments.size == 1) { "List should have 1 type argument" }

        val unboundedWildcard = itemsType.typeArguments[0]
        assert(unboundedWildcard is JavaWildcardType) {
            "Expected JavaWildcardType for ?, got ${unboundedWildcard?.javaClass}"
        }
        val unboundedType = unboundedWildcard as JavaWildcardType
        assert(unboundedType.isExtends) { "Unbounded wildcard should have isExtends=true" }
        assert(unboundedType.bound == null) { "Unbounded wildcard should have bound=null, got ${unboundedType.bound}" }

        // Test explicit extends Object: List<? extends Object>
        val extendsField = javaClass.fields.first { it.name.asString() == "explicitExtends" }
        val extendsType = extendsField.type as JavaClassifierType
        val extendsWildcard = extendsType.typeArguments[0] as JavaWildcardType
        assert(extendsWildcard.isExtends) { "? extends Object should have isExtends=true" }
        assert(extendsWildcard.bound != null) { "? extends Object should have a bound" }
        val extendsBound = extendsWildcard.bound as JavaClassifierType
        assert(extendsBound.classifierQualifiedName == "Object") {
            "Bound should be Object, got ${extendsBound.classifierQualifiedName}"
        }

        // Test super wildcard: List<? super String>
        val superField = javaClass.fields.first { it.name.asString() == "superWildcard" }
        val superType = superField.type as JavaClassifierType
        val superWildcard = superType.typeArguments[0] as JavaWildcardType
        assert(!superWildcard.isExtends) { "? super String should have isExtends=false" }
        assert(superWildcard.bound != null) { "? super String should have a bound" }
        val superBound = superWildcard.bound as JavaClassifierType
        assert(superBound.classifierQualifiedName == "String") {
            "Bound should be String, got ${superBound.classifierQualifiedName}"
        }
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

        assert(javaClass.typeParameters.size == 1) { "Generic should have 1 type parameter" }
        assert(javaClass.typeParameters.first().name.asString() == "T")

        // Check the static raw field
        val rawField = javaClass.fields.first { it.name.asString() == "raw" }
        val rawType = rawField.type as JavaClassifierType
        assert(rawType.classifierQualifiedName == "Generic") { "Expected 'Generic', got ${rawType.classifierQualifiedName}" }
        assert(rawType.typeArguments.isEmpty()) { "Raw type should have no type arguments" }
        // classifier should resolve to the containing class itself
        assert(rawType.classifier != null) { "classifier should resolve to Generic class" }
        assert(rawType.classifier == javaClass) { "classifier should be the same Generic class" }
        // isRaw should be true because Generic has type params but no args provided
        assert(rawType.isRaw) { "Expected isRaw=true for raw Generic field" }

        // Check the notRaw field (has explicit type argument)
        val notRawField = javaClass.fields.first { it.name.asString() == "notRaw" }
        val notRawType = notRawField.type as JavaClassifierType
        assert(notRawType.classifierQualifiedName == "Generic") { "Expected 'Generic', got ${notRawType.classifierQualifiedName}" }
        assert(notRawType.typeArguments.size == 1) { "notRaw should have 1 type argument, got ${notRawType.typeArguments.size}" }
        assert(!notRawType.isRaw) { "Expected isRaw=false for Generic<String>" }

        // Check the alsoRaw field (instance field, also raw)
        val alsoRawField = javaClass.fields.first { it.name.asString() == "alsoRaw" }
        val alsoRawType = alsoRawField.type as JavaClassifierType
        assert(alsoRawType.typeArguments.isEmpty()) { "alsoRaw should have no type arguments" }
        assert(alsoRawType.isRaw) { "Expected isRaw=true for raw alsoRaw field" }
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
        assert(fooParamType.classifier == null) { "External class List should have null classifier" }
        // classifierQualifiedName returns "List" (unresolved via star import)
        assert(fooParamType.classifierQualifiedName == "List") { "Expected 'List', got ${fooParamType.classifierQualifiedName}" }
        assert(fooParamType.typeArguments.isEmpty()) { "Raw List should have no type args" }
        // isRaw returns false for external classes because we can't determine type params without FIR
        // FIR's type conversion handles this via fallback logic
        // This documents the current behavior - java-direct can't determine isRaw for external classes
        assert(!fooParamType.isRaw) { "isRaw is false for external classes (FIR handles this)" }

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barParamType = barMethod.valueParameters.first().type as JavaClassifierType
        assert(barParamType.typeArguments.size == 1) { "List<String> should have 1 type arg" }
        assert(!barParamType.isRaw) { "List<String> should not be raw" }
    }
}
