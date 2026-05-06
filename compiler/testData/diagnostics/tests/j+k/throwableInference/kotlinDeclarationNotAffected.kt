// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: test.kt
// Kotlin declaration with unused Throwable-bounded type parameter
// should NOT be affected by this feature (Java-only)
fun <T, E : Throwable> kotlinCompute(action: () -> T): T = action()

fun test() {
    val result: String = <!CANNOT_INFER_PARAMETER_TYPE!>kotlinCompute<!> { "hello" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
