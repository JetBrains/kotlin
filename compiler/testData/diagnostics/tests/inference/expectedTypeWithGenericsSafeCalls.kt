// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ExpectedTypeFromCast

class X {
    fun <T> foo(): T = TODO()
}

fun test(x: X?) {
    val y = x?.foo() as Int
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, localProperty, nullableType,
propertyDeclaration, safeCall, typeParameter */
