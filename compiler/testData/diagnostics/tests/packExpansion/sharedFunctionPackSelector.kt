// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Text(text: String, color: String, modifier: Int = 0) {}

fun Text(text: Int, color: String, modifier: Int = 0) {}

fun Wrapper(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Text<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$sharedProps<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
    <!UNRESOLVED_REFERENCE!>modifier<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral */
