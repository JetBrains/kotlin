// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    javaClass.<!INVISIBLE_MEMBER!>doSomething<!> <!TYPE_MISMATCH!>{ }<!>
}

// FILE: JavaClass.java
public class JavaClass {
    private void doSomething(Runnable runnable) { runnable.run(); }
}