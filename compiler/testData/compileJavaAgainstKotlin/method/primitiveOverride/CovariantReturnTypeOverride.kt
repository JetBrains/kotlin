// FILE: CovariantReturnTypeOverride.java
package test;

class Test extends B {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}

// FILE: CovariantReturnTypeOverride.kt
package test

interface A {
    fun foo(): Any
}

open class B : A {
    override fun foo(): Int = 42
}
