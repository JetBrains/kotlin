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
    Derived::getA
    Derived::<!UNRESOLVED_REFERENCE!>a<!>
    x::getA
    x.getA("")
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