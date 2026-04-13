// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -InferThrowableTypeParameterToUpperBound
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
fun test() {
    // E is unused in the signature (only in throws), should be inferred to Throwable after the fix
    val result: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>compute<!> { "hello" }

    // Explicit type arguments should still work
    val result2: String = JavaHelper.compute<String, Throwable> { "hello" }

    // Single explicit argument should work as well
    val result3: String = JavaHelper.compute<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>> { "hello" }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral */
