// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

inline fun foo(x: () -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>x<!>.let {}
}

fun main() {
    foo {
        return@foo
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, lambdaLiteral */
