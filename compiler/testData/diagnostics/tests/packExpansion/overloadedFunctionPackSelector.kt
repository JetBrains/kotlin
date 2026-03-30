// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Props {
    val color: String = ""
}

fun Text(text: String, <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun Text(value: Int, <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Props<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun Wrapper(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Text<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
