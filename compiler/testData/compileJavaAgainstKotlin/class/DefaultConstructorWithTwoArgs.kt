// FILE: DefaultConstructorWithTwoArgs.java
package test;

class Simple {
    void foo() {
        new A();
    }
}

// FILE: DefaultConstructorWithTwoArgs.kt
package test

class A(val a: Int = 1, val b: String = "default")
