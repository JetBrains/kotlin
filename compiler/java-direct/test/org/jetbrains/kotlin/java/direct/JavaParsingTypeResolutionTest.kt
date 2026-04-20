/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test

class JavaParsingTypeResolutionTest : JavaParsingTestBase() {

    @Test
    fun testSupertypesAndTypeParameters() {
        val source = "class A<T> extends B implements C, D {}"
        val javaClass = parseFirstClass(source)

        assert(javaClass.typeParameters.size == 1)
        assert(javaClass.typeParameters.first().name.asString() == "T")

        assert(javaClass.supertypes.size == 3)
        val supertypeNames = javaClass.supertypes.map { it.classifierQualifiedName }
        assert(supertypeNames.contains("B"))
        assert(supertypeNames.contains("C"))
        assert(supertypeNames.contains("D"))
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
        assert(classes.size == 2) { "Expected 2 classes, got ${classes.size}" }

        val base = JavaClassOverAst(classes[0], tree, context)
        val derived = JavaClassOverAst(classes[1], tree, context)

        assert(base.name.asString() == "Base")
        assert(derived.name.asString() == "Derived")

        // Base has implicit java.lang.Object supertype
        assert(base.supertypes.size == 1) { "Base should have 1 supertype (implicit Object), got ${base.supertypes.size}" }
        assert(base.supertypes.first().classifierQualifiedName == "java.lang.Object") { "Base should extend Object" }
        
        assert(derived.supertypes.size == 1) { "Derived should have 1 supertype, got ${derived.supertypes.size}" }

        val supertype = derived.supertypes.first()
        assert(supertype.classifierQualifiedName == "Base") { "Expected Base, got ${supertype.classifierQualifiedName}" }

        val classifier = supertype.classifier
        assert(classifier != null) { "Expected classifier to be resolved" }
        assert(classifier is JavaClass) { "Expected JavaClass, got ${classifier?.javaClass}" }
        assert((classifier as JavaClass).name.asString() == "Base") { "Expected Base class, got ${classifier.name}" }
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
                    tree1.findChildByType(it, "IDENTIFIER")?.let { id -> tree1.getText(id).toString() } == "Derived"
        }
        val derived = JavaClassOverAst(derivedNode, tree1, context1)

        assert(derived.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype = derived.supertypes.first()
        assert(supertype.classifierQualifiedName == "Base") { "Expected 'Base', got '${supertype.classifierQualifiedName}'" }
        assert(supertype.classifier != null) { "Base should be resolved via local scope" }

        val sourceQualifiedName = """
            class MyClass extends java.util.ArrayList {}
        """.trimIndent()
        val myClass = parseFirstClass(sourceQualifiedName)

        assert(myClass.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype2 = myClass.supertypes.first()
        assert(supertype2.classifierQualifiedName == "java.util.ArrayList") { "Expected 'java.util.ArrayList', got '${supertype2.classifierQualifiedName}'" }
        assert(supertype2.classifier == null) { "java.util.ArrayList should NOT be in local scope" }
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

        assert(javaClass.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype = javaClass.supertypes.first()
        assert(supertype.classifierQualifiedName == "java.util.ArrayList") { "Expected qualified name java.util.ArrayList, got ${supertype.classifierQualifiedName}" }

        val listField = javaClass.fields.first { it.name.asString() == "list" }
        val listType = listField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(listType.classifierQualifiedName == "java.util.List") { "Expected qualified name java.util.List for list field, got ${listType.classifierQualifiedName}" }

        val counterField = javaClass.fields.first { it.name.asString() == "counter" }
        val counterType = counterField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(counterType.classifierQualifiedName == "AtomicInteger") { "Expected simple name AtomicInteger for star import, got ${counterType.classifierQualifiedName}" }
    }

    @Test
    fun testTypeResolution() {
        val source = """
            public class MyClass {
                public Object field;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assert(javaClass.fields.size == 1) { "Expected 1 field, got ${javaClass.fields.size}" }
        val field = javaClass.fields.first()
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType

        assert(fieldType.classifierQualifiedName == "Object") { "Expected 'Object', got '${fieldType.classifierQualifiedName}'" }
        assert(!fieldType.isResolved) { "Expected isResolved=false for unqualified Object" }
        assert(fieldType.classifier == null) { "Expected classifier=null for external type" }

        val resolved = fieldType.resolve(tryResolve = { candidateClassId ->
            candidateClassId == ClassId.topLevel(FqName("java.lang.Object"))
        })

        assert(resolved == ClassId.topLevel(FqName("java.lang.Object"))) { "Expected resolution to 'java.lang.Object', got '$resolved'" }
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
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(fieldType.classifier != null) { "Field type 'B' should have resolved classifier" }
        assert(fieldType.classifier?.name?.asString() == "B") { "Field type classifier should be 'B'" }

        val method = javaClass.methods.first { it.name.asString() == "method" }
        val returnType = method.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(returnType.classifier != null) { "Method return type 'B' should have resolved classifier" }
        assert(returnType.classifier?.name?.asString() == "B") { "Method return type classifier should be 'B'" }
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
        val type1 = field1.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type1.classifier != null) { "field1 type 'Inner' should resolve" }
        assert(type1.classifier?.name?.asString() == "Inner") { "field1 type should be 'Inner'" }

        val field2 = javaClass.fields.first { it.name.asString() == "field2" }
        val type2 = field2.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type2.classifier != null) { "field2 type 'Outer.Inner' should resolve" }
        assert(type2.classifier?.name?.asString() == "Inner") { "field2 type should be 'Inner'" }

        val field3 = javaClass.fields.first { it.name.asString() == "field3" }
        val type3 = field3.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type3.classifier != null) { "field3 type 'Outer.Inner.Deep' should resolve" }
        assert(type3.classifier?.name?.asString() == "Deep") { "field3 type should be 'Deep'" }

        val field4 = javaClass.fields.first { it.name.asString() == "field4" }
        val type4 = field4.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type4.classifier != null) { "field4 type 'Inner.Deep' should resolve" }
        assert(type4.classifier?.name?.asString() == "Deep") { "field4 type should be 'Deep'" }
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
        val nestedB = classA.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("b"))
        assert(nestedB != null) { "Should find nested class b in class a" }
        assert(nestedB!!.fqName?.asString() == "a.b") { "Nested class fqName should be 'a.b', got ${nestedB.fqName}" }
        
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
                tree2.findChildByType(it, "IDENTIFIER")?.let { id -> tree2.getText(id).toString() } == "c2"
            },
            tree2, context2
        )
        
        val getBMethod = c2Class.methods.first { it.name.asString() == "getB" }
        val returnType = getBMethod.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        println("Return type classifierQualifiedName: ${returnType.classifierQualifiedName}")
        println("Return type classifier: ${returnType.classifier}")
        println("Return type isResolved: ${returnType.isResolved}")
        
        // The return type "a.b" should resolve to nested class a.b (class a has priority over package a)
        assert(returnType.classifier != null) { "Return type 'a.b' should resolve to local nested class" }
        assert(returnType.classifier?.name?.asString() == "b") { "Classifier should be 'b'" }
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
        val returnType = getBMethod.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        println("Cross-file test:")
        println("  classifierQualifiedName: ${returnType.classifierQualifiedName}")
        println("  classifier: ${returnType.classifier}")
        println("  isResolved: ${returnType.isResolved}")
        
        // When class 'a' is NOT in the same file, classifier should be null (external)
        // and isResolved should be false (needs FIR resolution)
        assert(returnType.classifier == null) { "Classifier should be null for external type" }
        assert(!returnType.isResolved) { "Should not be resolved when class 'a' is in different file" }
        assert(returnType.classifierQualifiedName == "a.b") { "classifierQualifiedName should be 'a.b'" }
        
        // The key question: what does resolve() return when FIR calls it?
        // It should try both "class a with nested b" AND "package a with class b"
        // and let FIR determine which one exists
        
        var resolvedClassIds = mutableListOf<ClassId>()
        val resolved = returnType.resolve(tryResolve = { candidateClassId ->
            resolvedClassIds.add(candidateClassId)
            // Simulate: both a.b (package.class) and a.b (outer.nested) could exist
            // FIR would check which one actually exists
            false // Don't resolve, just collect candidates
        })

        println("  Candidates tried: $resolvedClassIds")
        
        // The resolve() should try "a.b" in some form
        assert(resolvedClassIds.isNotEmpty()) { "resolve() should try at least one candidate" }
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
        assert(implClass != null) { "Expected to find FunctionDescriptorImpl" }

        // Find CopyConfiguration
        val copyConfig = implClass!!.findInnerClass(Name.identifier("CopyConfiguration"))
        assert(copyConfig != null) { "Expected to find CopyConfiguration" }

        // CopyConfiguration should have SimpleFunctionDescriptor.CopyBuilder as a supertype
        val supertypes = copyConfig!!.supertypes.toList()
        assert(supertypes.isNotEmpty()) { "CopyConfiguration should have supertypes" }

        // First verify that findInnerClass on SimpleFunctionDescriptor finds CopyBuilder
        val simpleFuncDesc = outerClass.findInnerClass(Name.identifier("SimpleFunctionDescriptor"))
        assert(simpleFuncDesc != null) { "Expected to find SimpleFunctionDescriptor" }
        val inheritedCopyBuilder = simpleFuncDesc!!.findInnerClass(Name.identifier("CopyBuilder"))
        assert(inheritedCopyBuilder != null) {
            "SimpleFunctionDescriptor.findInnerClass('CopyBuilder') should find inherited inner class. " +
            "innerClassNames=${simpleFuncDesc.innerClassNames}"
        }

        // Now check the supertype resolution in the actual type reference
        val allQualifiedNames = supertypes.map { it.classifierQualifiedName }
        val copyBuilderSupertype = supertypes.find { it.classifierQualifiedName.contains("CopyBuilder") }
        assert(copyBuilderSupertype != null) {
            "Expected a supertype containing 'CopyBuilder', got supertypes: $allQualifiedNames"
        }

        val supertypeQualified = copyBuilderSupertype!!.classifierQualifiedName
        // Check classifierQualifiedName resolves the FQN properly
        assert(supertypeQualified != "SimpleFunctionDescriptor.CopyBuilder") {
            "classifierQualifiedName should resolve to the actual FQN, not raw text. " +
            "Got '$supertypeQualified'. This means classifierQualifiedName did not resolve via findInnerClass."
        }

        // Critical: the classifier should actually resolve (not be null)
        val classifier = copyBuilderSupertype.classifier
        assert(classifier != null) {
            "Expected supertype classifier to resolve for SimpleFunctionDescriptor.CopyBuilder " +
            "(inherited inner class). classifierQualifiedName='$supertypeQualified'"
        }
    }
}
