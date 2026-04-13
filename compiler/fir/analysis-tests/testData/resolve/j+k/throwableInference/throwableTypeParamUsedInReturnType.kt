// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: JavaHelper.java
public class JavaHelper {
    // E is used in only in the return type, should NOT be inferred even after the fix
    public static <E extends Throwable> E create() {
        return null;
    }
}

// FILE: test.kt
fun test() {
    val e = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>create<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, propertyDeclaration */
