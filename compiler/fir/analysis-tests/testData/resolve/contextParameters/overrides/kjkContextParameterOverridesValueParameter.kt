// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// FILE: A.kt
open class A {
    open fun foo(x: Int) = 5
}

class C : B() {
    context(x: Int)
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() = 6
}

// FILE: B.java
public class B extends A {
    @Override
    public int foo(int x) {
        return 10;
    }
}