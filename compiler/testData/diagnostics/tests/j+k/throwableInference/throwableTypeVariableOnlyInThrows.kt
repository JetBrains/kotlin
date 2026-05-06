// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: JavaHelper.java
public class JavaHelper {
    public static <E extends Throwable> void foo() throws E {}
}

// FILE: test.kt
fun test() {
    // We don't allow to infer E once it's not mentioned anywhere at all
    // (from Kotlin point-of-view `throws` section is invisible)
    JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>foo<!>()
    JavaHelper.foo<Throwable>()
    JavaHelper.foo<Exception>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, propertyDeclaration,
stringLiteral */
