// FILE: DefaultConstructor.java
package test;

class Simple {
    void foo() {
        new A();
    }
}

// FILE: DefaultConstructor.kt
package test

class A(val a: Int = 1)
