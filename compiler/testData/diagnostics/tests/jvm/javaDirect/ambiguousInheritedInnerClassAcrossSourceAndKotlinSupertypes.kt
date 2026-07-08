// RUN_PIPELINE_TILL: FRONTEND

// Regression test for a java-direct resolution-pipeline gap: `Sub` inherits two *different*
// nested classes both named `Foo` — one from a direct cross-file Java-source supertype
// (`JavaAncestor`), the other reachable only through a Kotlin supertype (`KotlinMiddle`) into a
// further Java-source ancestor (`JavaGrandparent`) — a Java-Kotlin-Java (j-k-j) shape. Per JLS
// 8.5, inheriting two unrelated member types with the same simple name is ambiguous, so `make()`'s
// return type must fail to resolve, matching javac's `MISSING_DEPENDENCY_CLASS`-shaped error.
//
// Before the fix, both the `ClassId`-returning and the structural (`JavaClass`-returning)
// resolution pipelines special-cased cross-file Java-source supertypes as a fast, classFinder-only
// path that returned as soon as it had a single non-ambiguous *source-only* answer, without ever
// checking whether a Kotlin/binary supertype at the same level had a conflicting one:
//  - `resolveInheritedInnerForLevel` (`JavaTypeResolver.kt`) found `JavaAncestor.Foo` via the
//    cached `collectInheritedInnerClasses` map and returned it directly, never running the BFS
//    that would have reached `KotlinMiddle` -> `JavaGrandparent.Foo`.
//  - `findInnerClassFromSupertypes` (`JavaInheritedMemberResolver.kt`) had the same shape: its
//    `classFinder`-backed cross-file-source arm returned before its separate binary/Kotlin tail
//    ever ran.
// Both are now merged into one origin-agnostic ladder that compares every candidate — regardless
// of whether it is same-file, cross-file source, Kotlin, or binary — before deciding.

// FILE: a/JavaAncestor.java
package a;

public interface JavaAncestor {
    class Foo {
        public void fromAncestor() {}
    }
}

// FILE: a/JavaGrandparent.java
package a;

public interface JavaGrandparent {
    class Foo {
        public void fromGrandparent() {}
    }
}

// FILE: b/KotlinMiddle.kt
package b

interface KotlinMiddle : a.JavaGrandparent

// FILE: c/Sub.java
package c;

import a.JavaAncestor;
import b.KotlinMiddle;

public interface Sub extends JavaAncestor, KotlinMiddle {
    Foo make();
}

// FILE: main.kt
import c.Sub

fun test(sub: Sub) {
    sub.<!MISSING_DEPENDENCY_CLASS!>make<!>().<!UNRESOLVED_REFERENCE!>fromAncestor<!>()
    sub.<!MISSING_DEPENDENCY_CLASS!>make<!>().<!UNRESOLVED_REFERENCE!>fromGrandparent<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, interfaceDeclaration, javaFunction, javaType */
