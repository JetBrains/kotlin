// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-92056

// FILE: ThrowableComputable.java
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: main.kt

typealias KotlinThrowableComputable<T, E> = ThrowableComputable<T, E>

fun foo() {
    ThrowableComputable<Int, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>> { 42 }
    KotlinThrowableComputable<Int, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>> { 42 }
    <!CANNOT_INFER_PARAMETER_TYPE!>ThrowableComputable<!> { 42 }
    <!CANNOT_INFER_PARAMETER_TYPE!>KotlinThrowableComputable<!> { 42 }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaType, lambdaLiteral, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
