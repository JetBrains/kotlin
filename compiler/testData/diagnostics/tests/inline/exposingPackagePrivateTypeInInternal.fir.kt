// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

// FILE: Private.java
interface Private {
    public static int foo = 1;
}

// FILE: test.kt
internal inline fun internal(arg: Any): Boolean = arg is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!> // should be an error

fun <T> ignore() {}

internal inline fun internal() {
    ignore<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>>() // should be an error
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>::class
    Private.<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING!>foo<!>
    Private::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING!>foo<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, nullableType, typeParameter */
