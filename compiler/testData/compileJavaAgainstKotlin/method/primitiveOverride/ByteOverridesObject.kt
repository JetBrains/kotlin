// FILE: ByteOverridesObject.java
package test;

class ExtendsB extends B {
    void test() {
        byte x = foo();
        Byte y = foo();
        Object z = foo();
    }
}

class ExtendsC extends C {
    void test() {
        byte x = foo();
        Byte y = foo();
        Object z = foo();
    }

    @Override
    public Byte foo() { return 42; }
}

// FILE: ByteOverridesObject.kt
package test

interface A<T> {
    fun foo(): T
}

open class B : A<Byte> {
    override fun foo(): Byte = 42
}

abstract class C : A<Byte>
