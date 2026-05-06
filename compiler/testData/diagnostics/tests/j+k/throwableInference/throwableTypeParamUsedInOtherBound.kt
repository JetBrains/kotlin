// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: JavaHelper.java
public class JavaHelper {
    // E is used in the bound of F (F extends Comparable<E>).
    // Even though E is Throwable-bounded and used only for SAM conversion
    // it IS used in another type parameter's bound, so it should NOT be inferred.
    public static <T, E extends Throwable, F extends Comparable<E>> T compute(F x, ThrowableComputable<T, E> value) {
        return value;
    }
}

// FILE: test.kt
fun test() {
    val result: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>compute<!>(null) { "hello" }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, propertyDeclaration,
stringLiteral */
