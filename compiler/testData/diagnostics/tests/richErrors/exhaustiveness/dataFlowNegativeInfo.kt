// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89434
// LANGUAGE: +DataFlowBasedExhaustiveness
error class Foo
error class Bar

fun earlyReturn(x: String | Foo | Bar): Int {
    if (x is Foo) return 0
    return when (x) {
        is String -> 1
        is Bar -> 2
    }
}

fun earlyReturnStillMissing(x: String | Foo | Bar): Int {
    if (x is Foo) return 0
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is String -> 1
    }
}

fun negativeBranch(x: String | Foo | Bar): Int {
    return if (x is Foo) {
        0
    } else {
        when (x) {
            is String -> 1
            is Bar -> 2
        }
    }
}

fun excludedPrimary(x: String | Foo): Int {
    if (x is String) return 0
    return when (x) {
        is Foo -> 1
    }
}

fun allErrorsExcluded(x: String | Foo | Bar): Int {
    if (x is RichError) return 0
    return when (x) {
        is String -> 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, isExpression, smartcast,
whenExpression, whenWithSubject */
