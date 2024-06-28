// FIR_IDENTICAL

// FILE: JavaClass.java
public class JavaClass {
    @Deprecated
    public int deprecatedField = 4;
    public int regularField = 5;
}

// FILE: use.kt
fun use(j: JavaClass) {
    j.<!DEPRECATION!>deprecatedField<!>
    j.regularField
}
