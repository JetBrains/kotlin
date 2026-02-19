// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaClass.java

public class JavaClass {
    public static String staticField;
    public String nonStaticField;
}

// FILE: test.kt

fun test() {
    val staticReference = JavaClass::staticField
    val nonStaticReference = JavaClass::nonStaticField
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaCallableReference, javaType, localProperty,
propertyDeclaration */
