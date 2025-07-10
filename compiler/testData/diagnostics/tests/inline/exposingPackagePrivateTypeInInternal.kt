// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

// FILE: Private.java
interface Private {}

// FILE: test.kt
internal inline fun internal(arg: Any): Boolean = arg is Private // should be an error

fun <T> ignore() {}

internal inline fun internal() {
    ignore<Private>() // should be an error
    Private::class
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, nullableType, typeParameter */
