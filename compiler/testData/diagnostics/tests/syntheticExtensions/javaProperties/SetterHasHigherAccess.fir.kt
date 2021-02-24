// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    val v = javaClass.<!HIDDEN!>something<!>
    javaClass.<!HIDDEN!>something<!> = 1
    javaClass.<!HIDDEN, HIDDEN!>something<!>++
}

// FILE: JavaClass.java
public class JavaClass {
    protected int getSomething() { return 1; }
    public void setSomething(int value) {}
}
