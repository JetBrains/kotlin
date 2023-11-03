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
    val pubFinRef: KProperty1<JavaClass, Int> = JavaClass::publicFinal
    val pubMutRef: KMutableProperty1<JavaClass, Long> = JavaClass::publicMutable
    val protFinRef: KProperty1<JavaClass, Double> = JavaClass::protectedFinal
    val protMutRef: KMutableProperty1<JavaClass, Char> = JavaClass::protectedMutable
    val privFinRef: KProperty1<JavaClass, String?> = JavaClass::<!INVISIBLE_REFERENCE!>privateFinal<!>
    val privMutRef: KMutableProperty1<JavaClass, Any?> = JavaClass::<!INVISIBLE_REFERENCE!>privateMutable<!>
}
