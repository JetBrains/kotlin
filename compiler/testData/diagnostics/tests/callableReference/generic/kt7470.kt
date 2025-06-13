// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE


fun <T> shuffle(x: List<T>): List<T> = x

fun bar() {
    val s: (List<String>) -> List<String> = ::shuffle
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, localProperty, nullableType,
propertyDeclaration, typeParameter */
