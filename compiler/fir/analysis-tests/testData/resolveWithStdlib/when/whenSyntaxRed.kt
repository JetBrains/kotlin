// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun describe(x: Int): String {
    return <!RETURN_TYPE_MISMATCH!>when(x) {
            1 <!SYNTAX!>"<!><!SYNTAX!><!SYNTAX!><!>One<!><!SYNTAX!><!SYNTAX!><!>"<!>   // <-- missing arrow
            2 <!SYNTAX!><!>-> "Two"
        else -> "many"
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
