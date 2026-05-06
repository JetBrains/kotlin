// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int) {}

fun bar() {
    <!NO_VALUE_FOR_PARAMETER!>foo<!>(<!NAMED_PARAMETER_NOT_FOUND!>y<!> = 1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
