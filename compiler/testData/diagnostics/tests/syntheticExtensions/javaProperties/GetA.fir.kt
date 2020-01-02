// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.a
    javaClass.<!UNRESOLVED_REFERENCE!>A<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public int getA() { return 1; }
}