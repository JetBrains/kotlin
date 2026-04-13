// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// FIR_DUMP
// ISSUE: KT-82961

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: JavaHelper.java
public class JavaHelper {
    // E is bounded by RuntimeException (a Throwable subclass), unused in signature
    public static <T, E extends RuntimeException> T computeRuntime(ThrowableComputable<T, E> action) throws E {
        return action.compute();
    }
}

// FILE: test.kt
fun test() {
    // E extends RuntimeException, becames unused after SAM conversion - inferred to RuntimeException
    val result: String = JavaHelper.computeRuntime { "hello" }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral */
