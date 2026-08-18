// FILE: OverrideInJava.java
package test;

class ExtendsB extends B {
    @Override
    public Integer foo() {
        return 239;
    }

    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}

// FILE: OverrideInJava.kt
package test

interface A<T> {
    fun foo(): T
}

abstract class B : A<Int> {
    override abstract fun foo(): Int
}
