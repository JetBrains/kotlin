// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-22520

// KT-22520: Expect functions are second-class to non-expect functions in overload resolution

<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> fun foo(x: Int)
fun <T> foo(x: T) {}

fun bar() {
    foo(1)  // Should resolve to expect foo(x: Int), but incorrectly resolves to foo(x: T)

    val f: (Int) -> Unit = ::foo
    // OVERLOAD_RESOLUTION_AMBIGUITY reported here, but shouldn't be
}

/* GENERATED_FIR_TAGS: callableReference, expect, functionDeclaration, functionalType, integerLiteral, localProperty,
nullableType, propertyDeclaration, typeParameter */
