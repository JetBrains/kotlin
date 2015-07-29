// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    javaClass.<!INVISIBLE_MEMBER!>doSomething<!> <!TYPE_MISMATCH!>{
        bar()
    }<!>
}

class X : JavaClass() {
    fun foo(other: JavaClass) {
        doSomething { bar() }
        other.doSomething { bar() } // currently not flagged as error - see KT-8654
    }
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    protected void doSomething(Runnable runnable) { runnable.run(); }
}