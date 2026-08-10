// FILE: FinalOverride.java
package test;

class Test extends B {
    void test() {
        A<Integer> a = new B();
        int ax = a.foo();
        Integer ay = a.foo();
        Object az = a.foo();

        B b = new B();
        int bx = b.foo();
        Integer by = b.foo();
        Object bz = b.foo();
    }
}

// FILE: FinalOverride.kt
package test

interface A<T> {
    fun foo(): T
}

open class B : A<Int> {
    override final fun foo(): Int = 42
}
