// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_IDENTICAL

inline fun foo(x: () -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>x<!>.let {}
}

fun main() {
    foo {
        return@foo
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, lambdaLiteral */
