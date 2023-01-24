// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    val v = javaClass.<!INVISIBLE_REFERENCE!>something<!>
    javaClass.<!INVISIBLE_REFERENCE!>something<!> = 1
    javaClass.<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>something<!>++
}

// FILE: JavaClass.java
public class JavaClass {
    protected int getSomething() { return 1; }
    public void setSomething(int value) {}
}
