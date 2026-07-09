// ISSUE: KT-87507
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: a
// FILE: a/b/c/Some.kt
package a.b.c

class Some

// MODULE: b(a)
// FILE: a/b/d/Base.java
package a.b.d;

import a.b.c.Some;

public abstract class Base {
    // classId: a/b/c/Some
    public abstract void foo(Some some);
}

// FILE: a/b/d/Derived.kt
package a.b.d

import a.b.c.Some

open class Derived : Base() {
    override fun foo(some: Some) {}
}

// MODULE: c(b)
// ^no dependency on `a`, it's important

import a.b.d.Derived

class Impl : Derived()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, override */
