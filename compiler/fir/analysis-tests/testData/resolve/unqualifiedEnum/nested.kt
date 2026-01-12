// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
enum class Outer {
    FIRST, SECOND;
}

enum class Inner {
    SECOND, THIRD;
}

fun foo(o: Outer, i: Inner): Int {
    return <!WHEN_ON_SEALED!>when (o) {
        FIRST -> 1
        SECOND -> <!WHEN_ON_SEALED!>when (i) {
            SECOND -> 2
            THIRD -> 3
        }<!>
    }<!>
}

fun bar(o: Outer, i: Inner): Int {
    return <!WHEN_ON_SEALED!>when (o) {
        FIRST -> 1
        SECOND -> {
            fun baz(): Int {
                return <!WHEN_ON_SEALED!>when (i) {
                    SECOND -> 2
                    THIRD -> 3
                }<!>
            }
            baz()
        }
    }<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, integerLiteral,
localFunction, smartcast, whenExpression, whenWithSubject */
