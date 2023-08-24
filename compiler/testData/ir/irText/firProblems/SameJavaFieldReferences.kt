// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// FILE: SomeJavaClass.java

public class SomeJavaClass {
    public static final String someJavaField = "Omega";
}

// FILE: SameJavaFieldReferences.kt

fun foo() {
    val ref1 = SomeJavaClass::someJavaField
    val ref2 = SomeJavaClass::someJavaField
}