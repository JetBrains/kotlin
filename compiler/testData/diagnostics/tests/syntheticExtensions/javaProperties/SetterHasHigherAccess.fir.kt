// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    val v = javaClass.<!INAPPLICABLE_CANDIDATE!>something<!>
    javaClass.<!INAPPLICABLE_CANDIDATE!>something<!> = 1
    javaClass.<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>something<!><!UNRESOLVED_REFERENCE!>++<!>
}

// FILE: JavaClass.java
public class JavaClass {
    protected int getSomething() { return 1; }
    public void setSomething(int value) {}
}