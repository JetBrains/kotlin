// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66612
// FILE: Base.kt
open class Base {
    open val String.a: Int
        get() = 42

    open val b: Int
        get() = 42
}

// FILE: Derived.java
public class Derived extends Base {
    @Override
    public int getA(String $this) {
        return -42;
    }

    @Override
    public int getB() {
        return -42;
    }
}

// FILE: main.kt
fun test(x: Derived) {
    Derived::<!UNRESOLVED_REFERENCE!>getA<!>
    Derived::<!UNRESOLVED_REFERENCE!>a<!>
    x::<!UNRESOLVED_REFERENCE!>getA<!>
    x.<!UNRESOLVED_REFERENCE!>getA<!>("")
    with(x) {
        "".a
    }

    Derived::<!UNRESOLVED_REFERENCE!>getB<!>
    Derived::b
    x::<!UNRESOLVED_REFERENCE!>getB<!>
    x::b
    x.<!UNRESOLVED_REFERENCE!>getB<!>()
    x.b
}