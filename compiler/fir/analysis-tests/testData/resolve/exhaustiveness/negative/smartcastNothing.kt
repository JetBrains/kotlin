// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class Enum { A, B }

fun test(e: Enum): Enum {
    if (e == Enum.A) return Enum.B
    if (e == Enum.B) return Enum.A
    return <!POTENTIALLY_NOTHING_VALUE!>e<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, smartcast */
