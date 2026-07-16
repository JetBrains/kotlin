// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC

// javac divergence — a qualified type name whose prefix is BOTH a class and a package
// (a discouraged "package/type name clash", JLS 6.1).
//
// `pkg.clash` is declared twice:
//   * as a top-level class     `pkg/clash.java`        -> class  `pkg.clash`
//   * as a package             `pkg/clash/Nested.java` -> class  `pkg.clash.Nested`
//
// The Java method `Provider.get()` returns the qualified type name `pkg.clash.Nested`.
//
// Strict JLS 6.5.4.2/6.5.5 (javac): resolving `pkg.clash.Nested` commits to the *type*
// `pkg.clash` at the leftmost point the qualifier becomes a type, then requires `Nested`
// to be a member type of it. It is not, so javac rejects `Provider.get()` with
// "cannot find symbol: class Nested, location: class clash" (the package `pkg.clash` is
// shadowed by the class of the same name).
//
// Both the PSI Java model and java-direct are LOOSE here: they fall back to the package
// interpretation and resolve `pkg.clash.Nested` to the top-level class `Nested` in package
// `pkg.clash`. This test pins that PSI == java-direct agreement — both diverge from javac
// identically — so `Provider.get()` returns `Nested` and `onlyOnNested()` resolves, while
// the class-only member `onlyOnClashClass()` does NOT.

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
    p.get().onlyOnNested()
    p.get().<!UNRESOLVED_REFERENCE!>onlyOnClashClass<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
