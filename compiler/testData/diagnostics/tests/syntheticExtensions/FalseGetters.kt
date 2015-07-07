// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.<!UNRESOLVED_REFERENCE!>something1<!>
    javaClass.<!UNRESOLVED_REFERENCE!>something2<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingStatic<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething1(int p) { return p; }
    public <T> T getSomething2() { return null; }

    public static int getSomethingStatic() { return 1; }
}