// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    javaClass.<!INVISIBLE_REFERENCE!>doSomething<!> {
        bar()
    }
}

class X : JavaClass() {
    fun foo(other: JavaClass) {
        doSomething { bar() }
        other.<!INVISIBLE_REFERENCE!>doSomething<!> { bar() }
    }
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    protected void doSomething(Runnable runnable) { runnable.run(); }
}
