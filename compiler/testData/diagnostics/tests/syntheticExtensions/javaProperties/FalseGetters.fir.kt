// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.<!UNRESOLVED_REFERENCE!>something1<!>
    javaClass.<!UNRESOLVED_REFERENCE!>something2<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingStatic<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingVoid<!>
    javaClass.<!UNRESOLVED_REFERENCE!>ter<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething1(int p) { return p; }

    public <T> T getSomething2() { return null; }

    public static int getSomethingStatic() { return 1; }

    public void getSomethingVoid() { }

    public int getter() { return 1; }
}
