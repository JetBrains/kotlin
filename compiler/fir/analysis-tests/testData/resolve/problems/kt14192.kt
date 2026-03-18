// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-14192

// KT-14192: No warning on platform class usage in static method call
fun test() {
    val x: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Integer<!>? = null   // warning (correct)

    Integer.parseInt("42")   // no warning (incorrect)

    val y = Integer::class   // no warning (incorrect)
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, javaFunction, localProperty, nullableType,
propertyDeclaration, stringLiteral */
