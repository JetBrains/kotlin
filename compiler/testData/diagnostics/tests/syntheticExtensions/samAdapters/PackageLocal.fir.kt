// FILE: KotlinFile1.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    javaClass.<!INVISIBLE_REFERENCE!>doSomething<!> { }
}

// FILE: KotlinFile2.kt
fun foo(javaClass: JavaClass) {
    javaClass.doSomething { }
}

// FILE: JavaClass.java
public class JavaClass {
    void doSomething(Runnable runnable) { runnable.run(); }
}
