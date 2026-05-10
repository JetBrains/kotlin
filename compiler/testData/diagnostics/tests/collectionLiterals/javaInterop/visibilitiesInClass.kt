// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// JDK_KIND: FULL_JDK_11
//  ^ to enable private static of

// FILE: test/Base.java
package test;

public class Base {
    public static Base of(int... x) {
        return null;
    }

    protected static Base of() {
        return null;
    }

    static Base of(int x) {
        return null;
    }

    private static Base of(int x, int y) {
        return null;
    }

    public static Base of(int x, int y, int z) {
        return null;
    }
}

// FILE: test/inSamePackage.kt
package test

fun test() {
    val a: Base = <!UNRESOLVED_REFERENCE!>[]<!>
    val b: Base = <!UNRESOLVED_REFERENCE!>[1]<!>
    val c: Base = <!UNRESOLVED_REFERENCE!>[1, 2]<!>
    val d: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
    val e: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3, 4]<!>
}

class Child : Base() {
    fun test() {
        val a: Base = <!UNRESOLVED_REFERENCE!>[]<!>
        val b: Base = <!UNRESOLVED_REFERENCE!>[1]<!>
        val c: Base = <!UNRESOLVED_REFERENCE!>[1, 2]<!>
        val d: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
        val e: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3, 4]<!>
    }
}

// FILE: other/inOtherPackage.kt
package other

import test.Base

fun test() {
    val a: Base = <!UNRESOLVED_REFERENCE!>[]<!>
    val b: Base = <!UNRESOLVED_REFERENCE!>[1]<!>
    val c: Base = <!UNRESOLVED_REFERENCE!>[1, 2]<!>
    val d: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
    val e: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3, 4]<!>
}

class Child : Base() {
    fun test() {
        val a: Base = <!UNRESOLVED_REFERENCE!>[]<!>
        val b: Base = <!UNRESOLVED_REFERENCE!>[1]<!>
        val c: Base = <!UNRESOLVED_REFERENCE!>[1, 2]<!>
        val d: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
        val e: Base = <!UNRESOLVED_REFERENCE!>[1, 2, 3, 4]<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaType, localProperty,
propertyDeclaration */
