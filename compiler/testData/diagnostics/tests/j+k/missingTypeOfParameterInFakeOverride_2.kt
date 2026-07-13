// LATEST_LV_DIFFERENCE
// ^this test requires deserialization of modules, and latestLV tests treat them as source dependencies
// ISSUE: KT-87507
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: a
// FILE: a/b/Some.kt
package a.b

class C {
    class Some
}

// MODULE: b(a)
// FILE: a/b/d/Base.java
package a.b.d;

import a.b.C.Some;

public abstract class Base {
    public abstract void foo(Some some);
}

// FILE: a/b/d/Derived.kt
package a.b.d

import a.b.C.Some

open class Derived : Base() {
    override fun foo(some: Some) {}
}

// MODULE: c(b)
// ^no dependency on `a`, it's important

import a.b.d.Derived

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Impl<!> : Derived()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, override */
