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
    ::publicFinal : KTopLevelProperty<String>
    ::publicMutable : KMutableTopLevelProperty<Any?>
    ::protectedFinal : KProperty<Double>
    ::protectedMutable : KMutableProperty<Char>
    ::<!INVISIBLE_MEMBER!>privateFinal<!> : KProperty<JavaClass?>
    ::<!INVISIBLE_MEMBER!>privateMutable<!> : KMutableProperty<Throwable?>
}
