// FILE: A.java

public class A {
    public A foo() {
        return this;
    }

    public A bar() {
        return this;
    }
}

// FILE: test.kt

class B : A() {
    override fun foo(): B = this
    fun <!VIRTUAL_MEMBER_HIDDEN!>bar<!>(): B = this // Here we should have "missing override" but no ambiguity

    fun test() {
        foo()
        bar()
    }
}
