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
    val pubFinRef: KMemberProperty<JavaClass, Int> = JavaClass::publicFinal
    val pubMutRef: KMutableMemberProperty<JavaClass, Long> = JavaClass::publicMutable
    val protFinRef: KMemberProperty<JavaClass, Double> = JavaClass::protectedFinal
    val protMutRef: KMutableMemberProperty<JavaClass, Char> = JavaClass::protectedMutable
    val privFinRef: KMemberProperty<JavaClass, String?> = JavaClass::<!INVISIBLE_MEMBER!>privateFinal<!>
    val privMutRef: KMutableMemberProperty<JavaClass, Any?> = JavaClass::<!INVISIBLE_MEMBER!>privateMutable<!>
}
