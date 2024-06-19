// WITH_STDLIB
// ISSUE: KT-68469
// FILE: Bar.java
public final class Bar {
    public Foo.Inner makeInner() {
        return new Foo.Inner() {};
    }
}

// FILE: Foo.kt
@file:JvmName("Foo")

class Foo {
    fun test(y: Bar): Inner {
        return <!RETURN_TYPE_MISMATCH!>y.<!MISSING_DEPENDENCY_CLASS!>makeInner<!>()<!>
    }

    interface Inner
}