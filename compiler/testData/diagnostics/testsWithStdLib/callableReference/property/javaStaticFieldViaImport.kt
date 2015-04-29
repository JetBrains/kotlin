// !CHECK_TYPE
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
    val pubFinRef: KTopLevelProperty<String> = ::publicFinal
    val pubMutRef: KMutableTopLevelProperty<Any?> = ::publicMutable
    val protFinRef: KProperty<Double> = ::protectedFinal
    val protMutRef: KMutableProperty<Char> = ::protectedMutable
    val privFinRef: KProperty<JavaClass?> = ::<!INVISIBLE_MEMBER!>privateFinal<!>
    val privMutRef: KMutableProperty<Throwable?> = ::<!INVISIBLE_MEMBER!>privateMutable<!>
}
