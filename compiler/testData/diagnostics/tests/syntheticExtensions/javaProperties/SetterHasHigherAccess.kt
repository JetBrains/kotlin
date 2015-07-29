// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    val <!UNUSED_VARIABLE!>v<!> = javaClass.<!INVISIBLE_MEMBER!>something<!>
    javaClass.<!INVISIBLE_MEMBER!>something<!> = 1
    javaClass.<!INVISIBLE_MEMBER!>something<!>++
}

// FILE: JavaClass.java
public class JavaClass {
    protected int getSomething() { return 1; }
    public void setSomething(int value) {}
}