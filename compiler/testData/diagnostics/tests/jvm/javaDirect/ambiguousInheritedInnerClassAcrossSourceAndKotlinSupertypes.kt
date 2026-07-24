// RUN_PIPELINE_TILL: FRONTEND

// `Sub` inherits two different nested classes named `Foo`: one from a direct Java-source
// supertype (`JavaAncestor`), the other reachable only through a Kotlin supertype
// (`KotlinMiddle`) from a further Java-source ancestor (`JavaGrandparent`). Per JLS 8.5,
// inheriting two unrelated member types with the same simple name is ambiguous, so `make()`'s
// return type must fail to resolve. Checks that candidates from Java-source and Kotlin/binary
// supertypes are compared for ambiguity together.

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
