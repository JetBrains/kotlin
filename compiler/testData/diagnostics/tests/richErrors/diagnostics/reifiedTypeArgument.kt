// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
inline fun <reified T> foo() {
    T::class
}

fun test() {
    foo<RichError>()
    foo<<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>NonError<!>>()
    foo<<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>String | RichError<!>>()
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, nullableType, reified, typeParameter */
