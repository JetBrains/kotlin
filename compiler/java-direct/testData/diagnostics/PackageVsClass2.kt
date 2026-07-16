// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// javac-strict mirror of `diagnostics/tests/javac/qualifiedExpression/PackageVsClass2.kt`,
// which is skipped for java-direct (see `SkipTestsPinningPsiJavaModelDeviationsMetaConfigurator`).
//
// In `test/d.java` the return type `a.b` is a package/type name clash (JLS 6.1): the class
// `test.a` is in scope and shadows the package `a`, so javac commits to the type `test.a`,
// requires `b` to be its member type, and rejects the reference ("cannot find symbol: class b,
// location: class a"). java-direct follows javac, leaving `getB()`'s return type unresolved,
// while the PSI Java model loosely resolves the package interpretation `a.b` (KT-87813).

// FILE: a/a.java
package a;

public class a {}

// FILE: a/b.java
package a;

public class b {
    public void a_b() {}
}

// FILE: test/a.java
package test;

public class a {}

// FILE: test/d.java
package test;

public class d {
    public a.b getB() { return null; }
}

// FILE: b.kt
package test

val x = d().<!MISSING_DEPENDENCY_CLASS!>getB<!>()

// FILE: test/c.java
package test;

import a.a;

public class c {
    public static a getA() { return null; }
}

// FILE: c.kt
package test

fun foo() {
    val a = c.getA()
    a.<!UNRESOLVED_REFERENCE!>a<!>
    a.<!UNRESOLVED_REFERENCE!>a<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, localProperty, propertyDeclaration */
