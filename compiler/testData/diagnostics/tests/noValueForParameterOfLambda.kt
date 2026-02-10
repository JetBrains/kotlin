// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_IDENTICAL
// ISSUE: KT-77401

fun test1(lambda: Int.(String) -> Unit) {
    lambda<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>p2<!> = "")
}

fun test2(lambda: Int.(s: String) -> Unit) {
    lambda<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>s<!> = "")
}

fun test3(lambda: Int.(@ParameterName("x") String) -> Unit) {
    lambda<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>x<!> = "")
}

const val NAME = "y"
fun test4(lambda: Int.(@ParameterName(NAME) String) -> Unit) {
    lambda<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    lambda(1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>y<!> = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, stringLiteral, typeWithExtension */
