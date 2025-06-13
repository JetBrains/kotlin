// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class Enum { A, B }

data class MyPair(val a: Enum, val b: Enum)

fun foo(p: MyPair): Int {
    if (p == MyPair(Enum.A, Enum.A)) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (p) {
        MyPair(Enum.A, Enum.B) -> 2
        MyPair(Enum.B, Enum.A) -> 3
        MyPair(Enum.B, Enum.B) -> 4
    }
}

fun bar(p: MyPair): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (p) {
        MyPair(Enum.A, Enum.A) -> 1
        MyPair(Enum.A, Enum.B) -> 2
        MyPair(Enum.B, Enum.A) -> 3
        MyPair(Enum.B, Enum.B) -> 4
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, primaryConstructor, propertyDeclaration, whenExpression, whenWithSubject */
