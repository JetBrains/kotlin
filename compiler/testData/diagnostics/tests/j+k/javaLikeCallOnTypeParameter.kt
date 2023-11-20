// FIR_IDENTICAL
// ISSUE: KT-63569

// FILE: Baz.java

public class Baz extends Base {
    public boolean isSomething() {
        return false;
    }
}

// FILE: Foo.kt

abstract class Base {
    open fun isSomethingElse() = false
}

abstract class Bar {
    open fun isSomething() = false
}

abstract class BazBaz : Baz()

abstract class Foo<C : Bar, D : Baz, E : D, F : BazBaz> {
    abstract val c: C
    abstract val d: D
    abstract val e: E
    abstract val f: F

    fun callMe() {
        c.<!FUNCTION_CALL_EXPECTED!>isSomething<!>
        d.isSomething
        d.<!FUNCTION_CALL_EXPECTED!>isSomethingElse<!>
        e.isSomething
        f.<!FUNCTION_CALL_EXPECTED!>isSomethingElse<!>
    }
}
