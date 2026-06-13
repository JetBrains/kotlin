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
    public static <T1, E1 extends Throwable> ThrowableComputable<T1, E1> wrapAsComputable(ThrowableComputable<T1, E1> t) { return t; }
    public static <T2, E2 extends Throwable> void consumeComputable(ThrowableComputable<T2, E2> x) {}
}

// FILE: test.kt
fun test() {
    // We don't allow inferring E1/E2 because they're mentioned in the return types
    val x1 = JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>wrapAsComputable<!> { 42 }
    val x2 = <!CANNOT_INFER_PARAMETER_TYPE!>ThrowableComputable<!> { 42 }

    // Working because default bounds constraints for E1 and E2 have been incorporated between them
    // and we allow to fix them to those new duplicated constraints
    JavaHelper.consumeComputable(JavaHelper.wrapAsComputable { 42 })
    JavaHelper.consumeComputable(ThrowableComputable { 42 })
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral */
