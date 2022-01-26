// FILE: KotlinFile.kt
class KotlinClass {
    public fun getSomething(): Int = 1
}

fun foo(javaClass: JavaClass, kotlinClass: KotlinClass) {
    useInt(javaClass.getSomething())
    useInt(javaClass.something)
    javaClass.<!VAL_REASSIGNMENT!>something<!> = 1
    javaClass.<!UNRESOLVED_REFERENCE!>Something<!>
    useInt(kotlinClass.getSomething())
    kotlinClass.<!UNRESOLVED_REFERENCE!>something<!>
}

fun useInt(i: Int) {}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
}
