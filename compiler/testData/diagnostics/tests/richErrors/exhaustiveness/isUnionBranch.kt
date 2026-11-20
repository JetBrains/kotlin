// RUN_PIPELINE_TILL: CODEGEN
error class E1
error class E2
error class E3

fun exhaustive(x: String | E1 | E2): String {
    return when (x) {
        is E1 | E2 -> "error"
        is String -> x
    }
}

fun exhaustiveNegative(x: String | E1 | E2): String {
    if (x is (E1 | E2)) return "error"
    return when (x) {
        is String -> x
    }
}

fun exhaustiveAfterSmartCase(o: Int | E1 | E2 | E3): Int {
    if (o !is (E1 | E2)) return 10

    return when (o) {
        is E1 -> 20
        is E2 -> 30
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, smartcast, stringLiteral,
whenExpression, whenWithSubject */
