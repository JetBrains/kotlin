// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(block: (Int) -> Unit) = Unit
fun foo(block: () -> Unit) = Unit

fun bar(lst: List<String>) = Unit
fun bar(lst: List<Int>) = Unit

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>({ <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> })
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(block = { <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> })

    <!NONE_APPLICABLE!>context<!>(with = ["!"], 42) { }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(lst = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    bar(lst = <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>())
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */
