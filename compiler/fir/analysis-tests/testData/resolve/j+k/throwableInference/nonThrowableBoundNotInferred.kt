// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: NonThrowableComputable.java
public interface NonThrowableComputable<T, E extends Number/* unused in SAM method */> {
    T compute();
}

// FILE: JavaHelper.java
public class JavaHelper {
    // E is NOT bounded by Throwable, should never be inferred by this feature
    public static <T, E extends Number> T doSomething(NonThrowableComputable<T, E> value) {
        return value;
    }
}

// FILE: test.kt
fun test() {
    // E is not Throwable-bounded, the feature should not apply
    val result: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>doSomething<!> { "hello" }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, propertyDeclaration,
stringLiteral */
