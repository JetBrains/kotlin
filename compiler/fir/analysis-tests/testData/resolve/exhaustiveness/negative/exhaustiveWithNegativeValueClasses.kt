// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class Enum { A, B }

@JvmInline
value class MyPair(val a: Enum)

fun foo(p: MyPair): Int {
    if (p == MyPair(Enum.A)) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (p) {
        MyPair(Enum.B) -> 2
    }
}

fun bar(p: MyPair): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (p) {
        MyPair(Enum.A) -> 1
        MyPair(Enum.B) -> 2
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, primaryConstructor, propertyDeclaration, value, whenExpression, whenWithSubject */
