// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-77401

fun test1(lambda: Int.(String) -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>lambda<!>(1)
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>p2<!> = "")
}

fun test2(lambda: Int.(s: String) -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>lambda<!>(1)
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>s<!> = "")
}

fun test3(lambda: Int.(@ParameterName("x") String) -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>lambda<!>(1)
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>x<!> = "")
}

const val NAME = "y"
fun test4(lambda: Int.(@ParameterName(NAME) String) -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>lambda<!>(1)
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>y<!> = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, stringLiteral, typeWithExtension */
