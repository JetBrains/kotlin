// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.somethingPublic
    javaClass.<!UNRESOLVED_REFERENCE!>somethingProtected<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingPrivate<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingPackage<!>
    <!VAL_REASSIGNMENT!>javaClass.somethingPublic<!> = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomethingPublic() { return 1; }
    protected int getSomethingProtected() { return 1; }
    private int getSomethingPrivate() { return 1; }
    int getSomethingPackage() { return 1; }

    protected void setSomethingPublic(int value) {}
}