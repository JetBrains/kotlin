// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class Enum { A, B, C }

fun foo(e: Enum): Int {
    if (e == Enum.A) return 1
    return when (e) {
        Enum.B -> 2
        Enum.C -> 3
    }
}

fun bar(e: Enum): Int {
    if (e == Enum.A) return 1
    if (e == Enum.B) return 2
    return when (e) {
        Enum.C -> 3
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
smartcast, whenExpression, whenWithSubject */
