// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    javaClass.somethingPublic
    javaClass.<!HIDDEN!>somethingProtected<!>
    javaClass.<!HIDDEN!>somethingPrivate<!>
    javaClass.<!HIDDEN!>somethingPackage<!>
    javaClass.somethingPublic = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomethingPublic() { return 1; }
    protected int getSomethingProtected() { return 1; }
    private int getSomethingPrivate() { return 1; }
    int getSomethingPackage() { return 1; }

    protected void setSomethingPublic(int value) {}
}
