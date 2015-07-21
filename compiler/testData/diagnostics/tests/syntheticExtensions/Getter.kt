// FILE: KotlinFile.kt
class KotlinClass {
    public fun getSomething(): Int = 1
}

fun foo(javaClass: JavaClass, kotlinClass: KotlinClass) {
    useInt(javaClass.getSomething())
    useInt(javaClass.something)
    <!VAL_REASSIGNMENT!>javaClass.something<!> = 1
    javaClass.<!UNRESOLVED_REFERENCE!>Something<!>
    useInt(kotlinClass.getSomething())
    kotlinClass.<!UNRESOLVED_REFERENCE!>something<!>
}

fun useInt(<!UNUSED_PARAMETER!>i<!>: Int) {}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
}