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

fun test() {
    JavaClass::publicFinal : KMemberProperty<JavaClass, Int>
    JavaClass::publicMutable : KMutableMemberProperty<JavaClass, Long>
    JavaClass::protectedFinal : KMemberProperty<JavaClass, Double>
    JavaClass::protectedMutable : KMutableMemberProperty<JavaClass, Char>
    JavaClass::<!INVISIBLE_MEMBER!>privateFinal<!> : KMemberProperty<JavaClass, String?>
    JavaClass::<!INVISIBLE_MEMBER!>privateMutable<!> : KMutableMemberProperty<JavaClass, Any?>
}
