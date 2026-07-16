// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC

// A qualified type name whose prefix is BOTH a class and a package (a discouraged
// "package/type name clash", JLS 6.1).
//
// `pkg.clash` is declared twice:
//   * as a top-level class     `pkg/clash.java`        -> class  `pkg.clash`
//   * as a package             `pkg/clash/Nested.java` -> class  `pkg.clash.Nested`
//
// The Java method `Provider.get()` returns the qualified type name `pkg.clash.Nested`.
//
// Strict JLS 6.5.4.2/6.5.5 (and javac): resolving `pkg.clash.Nested` commits to the *type*
// `pkg.clash` at the leftmost point the qualifier becomes a type, then requires `Nested`
// to be a member type of it. It is not, so javac rejects `Provider.get()` with
// "cannot find symbol: class Nested, location: class clash" (the package `pkg.clash` is
// shadowed by the class of the same name).
//
// java-direct follows javac: the committed interpretation stays unresolved, so `Provider.get()`
// has an unresolved return type and neither member call resolves (red code). This diverges from
// the PSI Java model, which loosely falls back to the package interpretation and resolves
// `pkg.clash.Nested` to the top-level class `Nested` — which is why this test lives in the
// java-direct-owned testdata root rather than the shared diagnostics roots (KT-87813).

// FILE: pkg/clash.java
package pkg;

public class clash {
    public int onlyOnClashClass() { return 1; }
}

// FILE: pkg/clash/Nested.java
package pkg.clash;

public class Nested {
    public int onlyOnNested() { return 2; }
}

// FILE: user/Provider.java
package user;

public class Provider {
    public pkg.clash.Nested get() { return null; }
}

// FILE: main.kt
package main

import user.Provider

fun test(p: Provider) {
    p.<!MISSING_DEPENDENCY_CLASS!>get<!>().<!UNRESOLVED_REFERENCE!>onlyOnNested<!>()
    p.<!MISSING_DEPENDENCY_CLASS!>get<!>().<!UNRESOLVED_REFERENCE!>onlyOnClashClass<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
