// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Text(text: String, color: String, modifier: Int = 0) {}

fun Text(text: Int, color: String, modifier: Int = 0) {}

fun Wrapper(...Text.$sharedProps) {
    <!UNRESOLVED_REFERENCE!>text<!>
    color.length
    modifier + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral */
