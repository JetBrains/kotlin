/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.resolution.findInnerClassFromSupertypes
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class JavaParsingTypeResolutionTest : JavaParsingTestBase() {

    @Test
    fun testSupertypesAndTypeParameters() {
        val source = "class A<T> extends B implements C, D {}"
        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.typeParameters.size)
        assertEquals("T", javaClass.typeParameters.first().name.asString())

        assertEquals(3, javaClass.supertypes.size)
        val supertypeNames = javaClass.supertypes.map { it.classifierQualifiedName }
        assertTrue(supertypeNames.contains("B")) { "Supertypes: $supertypeNames" }
        assertTrue(supertypeNames.contains("C")) { "Supertypes: $supertypeNames" }
        assertTrue(supertypeNames.contains("D")) { "Supertypes: $supertypeNames" }
    }

    @Test
    fun testInheritedInnerClassFromNestedGenericSupertype() {
        val source = """
            class Outer<T> {
                class Inner {
                    class Target {}
                }
            }
            class Derived extends Outer<String>.Inner {}
        """.trimIndent()

        val parsed = parseSource(source)
        val tree = parsed.tree
        val context = parsed.context
        val topLevelClasses: Map<String, JavaClassOverAst> = tree.getChildren(parsed.root)
            .filter { tree.getType(it).toString() == "CLASS" }
            .associate { node ->
                val cls = JavaClassOverAst(node, tree, context)
                cls.name.asString() to cls
            }

        val derived = topLevelClasses.getValue("Derived")

        val found = with(context) {
            findInnerClassFromSupertypes(Name.identifier("Target"), derived)
        }
        assertNotNull(found) {
            "Expected to resolve inherited inner class 'Target' through nested generic supertype " +
                    "Outer<String>.Inner, but resolution returned null"
        }
        assertEquals("Target", found.name.asString())
    }

    @Test
    fun testLocalInheritance() {
        val source = """
            class Base {}
            class Derived extends Base {}
        """.trimIndent()
        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context

        val classes = tree.getChildren(root).filter { tree.getType(it).toString() == "CLASS" }
        assertEquals(2, classes.size)

        val base = JavaClassOverAst(classes[0], tree, context)
        val derived = JavaClassOverAst(classes[1], tree, context)

        assertEquals("Base", base.name.asString())
        assertEquals("Derived", derived.name.asString())

        // Base has implicit java.lang.Object supertype
        assertEquals(1, base.supertypes.size) { "Base should have exactly the implicit Object supertype" }
        assertEquals("java.lang.Object", base.supertypes.first().classifierQualifiedName)

        assertEquals(1, derived.supertypes.size)

        val supertype = derived.supertypes.first()
        assertEquals("Base", supertype.classifierQualifiedName)

        val classifier = supertype.classifier
        assertNotNull(classifier) { "Expected classifier to be resolved" }
        assertTrue(classifier is JavaClass) { "Expected JavaClass, got ${classifier.javaClass}" }
        assertEquals("Base", (classifier as JavaClass).name.asString())
    }

    @Test
    fun testClassifierQualifiedName() {
        val sourceSimpleName = """
            class Base {}
            class Derived extends Base {}
        """.trimIndent()
        val parsed1 = parseSource(sourceSimpleName)
        val root1 = parsed1.root
        val tree1 = parsed1.tree
        val context1 = parsed1.context

        val derivedNode = tree1.getChildren(root1).first {
            tree1.getType(it).toString() == "CLASS" &&
                    tree1.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree1.getText(id).toString() } == "Derived"
        }
        val derived = JavaClassOverAst(derivedNode, tree1, context1)

        assertEquals(1, derived.supertypes.size)
        val supertype = derived.supertypes.first()
        assertEquals("Base", supertype.classifierQualifiedName)
        assertNotNull(supertype.classifier) { "Base should be resolved via local scope" }

        val sourceQualifiedName = """
            class MyClass extends java.util.ArrayList {}
        """.trimIndent()
        val myClass = parseFirstClass(sourceQualifiedName)

        assertEquals(1, myClass.supertypes.size)
        val supertype2 = myClass.supertypes.first()
        assertEquals("java.util.ArrayList", supertype2.classifierQualifiedName)
        assertNull(supertype2.classifier) { "java.util.ArrayList should NOT be in local scope" }
    }

    @Test
    fun testImportExtraction() {
        val source = """
            package test;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.concurrent.atomic.*;
            
            class MyClass extends ArrayList {
                List list;
                AtomicInteger counter;
            }
        """.trimIndent()

        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.supertypes.size)
        val supertype = javaClass.supertypes.first()
        assertEquals("ArrayList", supertype.classifierQualifiedName)

        val listField = javaClass.fields.first { it.name.asString() == "list" }
        val listType = listField.type as JavaClassifierType
        assertEquals("List", listType.classifierQualifiedName)

        val counterField = javaClass.fields.first { it.name.asString() == "counter" }
        val counterType = counterField.type as JavaClassifierType
        assertEquals("AtomicInteger", counterType.classifierQualifiedName) { "The star-imported type should keep its simple name" }
    }

    @Test
    fun testTypeResolution() {
        val source = """
            public class MyClass {
                public Object field;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.fields.size)
        val field = javaClass.fields.first()
        val fieldType = field.type as JavaClassifierType

        assertEquals("Object", fieldType.classifierQualifiedName)
        assertNull(fieldType.classifier) { "Expected classifier=null for external type without a wired symbol provider" }
    }

    @Test
    fun testLocalTypeResolutionInMembers() {
        val source = """
            public class A {
                public class B {}
                public B field;
                public B method() { return null; }
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "field" }
        val fieldType = field.type as JavaClassifierType
        assertNotNull(fieldType.classifier) { "Field type 'B' should have resolved classifier" }
        assertEquals("B", fieldType.classifier?.name?.asString())

        val method = javaClass.methods.first { it.name.asString() == "method" }
        val returnType = method.returnType as JavaClassifierType
        assertNotNull(returnType.classifier) { "Method return type 'B' should have resolved classifier" }
        assertEquals("B", returnType.classifier?.name?.asString())
    }

    @Test
    fun testNestedClassResolution() {
        val source = """
            public class Outer {
                public class Inner {
                    public class Deep {
                    }
                }
                
                public Inner field1;
                public Outer.Inner field2;
                public Outer.Inner.Deep field3;
                public Inner.Deep field4;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field1 = javaClass.fields.first { it.name.asString() == "field1" }
        val type1 = field1.type as JavaClassifierType
        assertNotNull(type1.classifier) { "field1 type 'Inner' should resolve" }
        assertEquals("Inner", type1.classifier?.name?.asString())

        val field2 = javaClass.fields.first { it.name.asString() == "field2" }
        val type2 = field2.type as JavaClassifierType
        assertNotNull(type2.classifier) { "field2 type 'Outer.Inner' should resolve" }
        assertEquals("Inner", type2.classifier?.name?.asString())

        val field3 = javaClass.fields.first { it.name.asString() == "field3" }
        val type3 = field3.type as JavaClassifierType
        assertNotNull(type3.classifier) { "field3 type 'Outer.Inner.Deep' should resolve" }
        assertEquals("Deep", type3.classifier?.name?.asString())

        val field4 = javaClass.fields.first { it.name.asString() == "field4" }
        val type4 = field4.type as JavaClassifierType
        assertNotNull(type4.classifier) { "field4 type 'Inner.Deep' should resolve" }
        assertEquals("Deep", type4.classifier?.name?.asString())
    }

    @Test
    fun testQualifiedTypeResolutionClassVsPackage() {
        // When a qualified name like "a.b" could refer to either:
        // - package a, class b
        // - class a, nested class b
        // Java resolves to "class a, nested class b" (class takes priority)
        val source = """
            // This simulates class a with nested class b
            public class a {
                public class b {
                    public void nestedMethod() {}
                }
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context
        val classA = JavaClassOverAst(tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }, tree, context)

        // Verify we can find nested class b
        val nestedB = classA.findInnerClass(Name.identifier("b"))
        assertNotNull(nestedB) { "Should find nested class b in class a" }
        assertEquals("a.b", nestedB.fqName?.asString())

        // Now test resolution of "a.b" as a type reference in another class (same file)
        val source2 = """
            public class c2 {
                public a.b getB() { return null; }
            }
            
            public class a {
                public class b {}
            }
        """.trimIndent()

        val parsed2 = parseSource(source2)
        val root2 = parsed2.root
        val tree2 = parsed2.tree
        val context2 = parsed2.context
        val classes = tree2.getChildren(root2).filter { tree2.getType(it).toString() == "CLASS" }
        val c2Class = JavaClassOverAst(
            classes.first {
                tree2.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree2.getText(id).toString() } == "c2"
            },
            tree2, context2
        )

        val getBMethod = c2Class.methods.first { it.name.asString() == "getB" }
        val returnType = getBMethod.returnType as JavaClassifierType


        // The return type "a.b" should resolve to nested class a.b (class a has priority over package a)
        assertNotNull(returnType.classifier) { "Return type 'a.b' should resolve to local nested class" }
        assertEquals("b", returnType.classifier?.name?.asString())
    }

    @Test
    fun testQualifiedTypeResolutionCrossFile() {
        // Test cross-file scenario: c2.java references a.b where class a is in another file
        // This is the scenario that fails in TopLevelClassVsPackage test

        // c2.java - uses qualified a.b, class a is NOT in this file
        val sourceC2 = """
            public class c2 {
                public a.b getB() { return null; }
            }
        """.trimIndent()

        val parsedC2 = parseSource(sourceC2)
        val rootC2 = parsedC2.root
        val treeC2 = parsedC2.tree
        val contextC2 = parsedC2.context
        val c2Class = JavaClassOverAst(
            treeC2.getChildren(rootC2).first { treeC2.getType(it).toString() == "CLASS" },
            treeC2, contextC2
        )

        val getBMethod = c2Class.methods.first { it.name.asString() == "getB" }
        val returnType = getBMethod.returnType as JavaClassifierType

        // When class 'a' is NOT in the same file, classifier should be null (external,
        // parsing-level fixture has no `FirSession` wired so the cross-file branch
        // short-circuits per Step 4.5b).
        assertNull(returnType.classifier) { "Classifier should be null for external type" }
        assertEquals("a.b", returnType.classifierQualifiedName)
    }

    @Test
    fun testInheritedInnerClassResolution() {
        // Reproduces the FunctionDescriptor/SimpleFunctionDescriptor/FunctionDescriptorImpl hierarchy:
        // - FunctionDescriptor declares inner interface CopyBuilder
        // - SimpleFunctionDescriptor extends FunctionDescriptor (inherits CopyBuilder)
        // - FunctionDescriptorImpl.CopyConfiguration implements SimpleFunctionDescriptor.CopyBuilder
        //   (where CopyBuilder is inherited, not directly declared in SimpleFunctionDescriptor)
        val source = """
            public class TestInheritedInner {
                public interface FunctionDescriptor {
                    interface CopyBuilder<D> {}
                }
                
                public interface SimpleFunctionDescriptor extends FunctionDescriptor {
                    // CopyBuilder is inherited from FunctionDescriptor, NOT declared here
                }
                
                public abstract class FunctionDescriptorImpl implements FunctionDescriptor {
                    // CopyConfiguration references SimpleFunctionDescriptor.CopyBuilder
                    // which is inherited, not directly declared
                    public class CopyConfiguration implements SimpleFunctionDescriptor.CopyBuilder<FunctionDescriptor> {
                    }
                }
            }
        """.trimIndent()
        val outerClass = parseFirstClass(source)

        // Find FunctionDescriptorImpl
        val implClass = outerClass.findInnerClass(Name.identifier("FunctionDescriptorImpl"))
        assertNotNull(implClass) { "Expected to find FunctionDescriptorImpl" }

        // Find CopyConfiguration
        val copyConfig = implClass.findInnerClass(Name.identifier("CopyConfiguration"))
        assertNotNull(copyConfig) { "Expected to find CopyConfiguration" }

        // CopyConfiguration should have SimpleFunctionDescriptor.CopyBuilder as a supertype
        val supertypes = copyConfig.supertypes.toList()
        assertTrue(supertypes.isNotEmpty()) { "CopyConfiguration should have supertypes" }

        // Declared-only contract: findInnerClass returns ONLY directly declared member types,
        // matching JavaClassImpl (PSI) / BinaryJavaClass. CopyBuilder is inherited (declared in
        // FunctionDescriptor), so a direct findInnerClass on SimpleFunctionDescriptor must NOT find it;
        // inherited lookup is the resolution layer's job (validated via the type reference below).
        val simpleFuncDesc = outerClass.findInnerClass(Name.identifier("SimpleFunctionDescriptor"))
        assertNotNull(simpleFuncDesc) { "Expected to find SimpleFunctionDescriptor" }
        val inheritedCopyBuilder = simpleFuncDesc.findInnerClass(Name.identifier("CopyBuilder"))
        assertNull(inheritedCopyBuilder) {
            "SimpleFunctionDescriptor.findInnerClass('CopyBuilder') must return null for an inherited " +
                    "(not directly declared) member type. innerClassNames=${simpleFuncDesc.innerClassNames}"
        }

        // Inherited resolution must still work end-to-end through the resolver / type-reference path:
        // CopyConfiguration's supertype `SimpleFunctionDescriptor.CopyBuilder` resolves CopyBuilder as a
        // member type inherited by SimpleFunctionDescriptor from FunctionDescriptor.
        val allQualifiedNames = supertypes.map { it.classifierQualifiedName }
        val copyBuilderSupertype = supertypes.find { it.classifierQualifiedName.contains("CopyBuilder") }
        assertNotNull(copyBuilderSupertype) {
            "Expected a supertype containing 'CopyBuilder', got supertypes: $allQualifiedNames"
        }

        val supertypeQualified = copyBuilderSupertype.classifierQualifiedName
        // Check classifierQualifiedName resolves the FQN properly
        assertNotEquals("SimpleFunctionDescriptor.CopyBuilder", supertypeQualified) {
            "classifierQualifiedName should resolve to the actual FQN, not raw text. " +
                    "This means classifierQualifiedName did not resolve via findInnerClass."
        }

        // Critical: the classifier should actually resolve (not be null)
        val classifier = copyBuilderSupertype.classifier
        assertNotNull(classifier) {
            "Expected supertype classifier to resolve for SimpleFunctionDescriptor.CopyBuilder " +
                    "(inherited inner class). classifierQualifiedName='$supertypeQualified'"
        }
    }

    @Test
    fun testInheritedInnerClassFromQualifiedNestedSameFileSupertype() {
        // Regression for the fragile `substringBefore('.')` supertype-ref shortcut in the same-file
        // supertype walk: here the supertype is a *qualified-nested* same-file class `x.S`, and `B`
        // is a member type declared in `x.S` and inherited by `x1`. Resolving the bare `B` in x1's
        // scope must navigate the full `x.S` reference (resolve `x`, then `.S`) — the old shortcut
        // stopped at the outer class `x` and missed `B`.
        val source = """
            public class x {
                public static class S {
                    public static class B {}
                }
            }
            public class x1 extends x.S {
                public B getB() { return null; }
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context

        val x1Node = tree.getChildren(root).first {
            tree.getType(it).toString() == "CLASS" &&
                    tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "x1"
        }
        val x1Class = JavaClassOverAst(x1Node, tree, context)

        val getBMethod = x1Class.methods.first { it.name.asString() == "getB" }
        val returnType = getBMethod.returnType as JavaClassifierType

        // `B` is inherited from the qualified-nested supertype `x.S`; the same-file supertype walk
        // must resolve it by navigating the full reference, not just its first segment.
        assertNotNull(returnType.classifier) {
            "Return type 'B' should resolve to the inherited nested class x.S.B via the same-file " +
                    "supertype walk, but classifier was null " +
                    "(classifierQualifiedName='${returnType.classifierQualifiedName}')"
        }
        assertEquals("B", returnType.classifier?.name?.asString())
    }
}
