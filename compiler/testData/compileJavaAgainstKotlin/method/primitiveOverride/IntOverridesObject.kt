// FILE: IntOverridesObject.java
package test;

class ExtendsB extends B {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }
}

class ExtendsC extends C {
    void test() {
        int x = foo();
        Integer y = foo();
        Object z = foo();
    }

    @Override
    public Integer foo() { return 42; }
}

// FILE: IntOverridesObject.kt
package test

interface A<T> {
    fun foo(): T
}

open class B : A<Int> {
    override fun foo(): Int = 42
}

abstract class C : A<Int>
