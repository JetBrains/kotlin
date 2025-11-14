// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

enum class E { A, B }

fun test(e: E): String =
    when (e) {
        E.A -> "a"
        E.B -> "b"
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "other" // all cases are covered so this will never happen
    }

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
