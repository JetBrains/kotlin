// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// FULL_JDK
// WITH_STDLIB
// FIR_DUMP
// ISSUE: KT-82961

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: JavaHelper.java
public class JavaHelper {
    public static <T, E1 extends Throwable, E2 extends Exception> T computeMulti(ThrowableComputable<T, E1> action) throws E1, E2 {
        return action.compute();
    }

    public static <T, E1 extends Throwable, E2 extends Exception> T computeMixed(ThrowableComputable<T, E1> action, Class<E2> exClass) throws E1, E2 {
        return action.compute();
    }
}

// FILE: test.kt
fun test() {
    // E1 is unused in a function type for SAM, E2 is completely unused
    val result: String = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>computeMulti<!> { "hello" }

    // E1 is unused in a function type for SAM, E2 is used in Class<E2>
    val result2: String = JavaHelper.computeMixed({ "hello" }, Exception::class.java)
}

/* GENERATED_FIR_TAGS: classReference, flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral,
localProperty, propertyDeclaration, samConversion, stringLiteral */
