// !DIAGNOSTICS:-UNUSED_VARIABLE
// FILE: JavaClass.java

public class JavaClass {
    public final int publicFinal;
    public long publicMutable;

    protected final double protectedFinal;
    protected char protectedMutable;

    private final String privateFinal;
    private Object privateMutable;
}

// FILE: test.kt

import kotlin.reflect.*

fun test() {
    val pubFinRef: KProperty1<JavaClass, Int> = <!UNRESOLVED_REFERENCE!>JavaClass::publicFinal<!>
    val pubMutRef: KMutableProperty1<JavaClass, Long> = <!UNRESOLVED_REFERENCE!>JavaClass::publicMutable<!>
    val protFinRef: KProperty1<JavaClass, Double> = <!UNRESOLVED_REFERENCE!>JavaClass::protectedFinal<!>
    val protMutRef: KMutableProperty1<JavaClass, Char> = <!UNRESOLVED_REFERENCE!>JavaClass::protectedMutable<!>
    val privFinRef: KProperty1<JavaClass, String?> = <!UNRESOLVED_REFERENCE!>JavaClass::privateFinal<!>
    val privMutRef: KMutableProperty1<JavaClass, Any?> = <!UNRESOLVED_REFERENCE!>JavaClass::privateMutable<!>
}
