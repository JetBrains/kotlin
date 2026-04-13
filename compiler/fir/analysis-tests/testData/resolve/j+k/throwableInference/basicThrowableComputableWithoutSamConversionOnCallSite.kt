// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// FIR_DUMP
// ISSUE: KT-82961

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: JavaHelper.java
public class JavaHelper {
    public static <T, E extends Throwable> T compute(ThrowableComputable<T, E> action) throws E {
        return action.compute();
    }
}

// FILE: test.kt
fun <T> materialize(): T = TODO()

fun test(x: ThrowableComputable<*, *>) {
    // E is generally acceptable to be auto-inferred, but here, no SAM conversion is applied
    val result1: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>compute<!>(null)
    val result2: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>compute<!>(<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>())
    val result3: Any = JavaHelper.compute(x)

    // Explicit type arguments should still work
    val result4: String = JavaHelper.compute<String, Throwable>(null)
    val result5: String = JavaHelper.compute<String, Throwable>(materialize())
    val result6: Any = JavaHelper.compute<String, Throwable>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    val result7: String = JavaHelper.compute<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>>(null)
    val result8: String = JavaHelper.compute<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>>(<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>())
    val result9: Any = JavaHelper.compute<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral */
