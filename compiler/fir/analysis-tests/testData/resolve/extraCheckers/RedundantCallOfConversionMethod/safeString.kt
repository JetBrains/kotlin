// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test() {
    val foo: String? = null
    foo?.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration, safeCall */
