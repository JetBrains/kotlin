// !DIAGNOSTICS:-UNUSED_VARIABLE
// FILE: JavaClass.java

public class JavaClass {
    public static final String publicFinal;
    public static volatile Object publicMutable;

    protected static final double protectedFinal;
    protected static char protectedMutable;

    private static final JavaClass privateFinal;
    private static Throwable privateMutable;
}

// FILE: test.kt

import JavaClass.*

import kotlin.reflect.*

fun test() {
    val pubFinRef: KProperty0<String> = <!UNRESOLVED_REFERENCE!>::publicFinal<!>
    val pubMutRef: KMutableProperty0<Any?> = <!UNRESOLVED_REFERENCE!>::publicMutable<!>
    val protFinRef: KProperty<Double> = <!UNRESOLVED_REFERENCE!>::protectedFinal<!>
    val protMutRef: KMutableProperty<Char> = <!UNRESOLVED_REFERENCE!>::protectedMutable<!>
    val privFinRef: KProperty<JavaClass?> = <!UNRESOLVED_REFERENCE!>::privateFinal<!>
    val privMutRef: KMutableProperty<Throwable?> = <!UNRESOLVED_REFERENCE!>::privateMutable<!>
}
