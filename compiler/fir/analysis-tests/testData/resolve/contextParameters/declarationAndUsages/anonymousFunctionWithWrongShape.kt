// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun runWithA(block: context(String) () -> Unit) {
}

fun test() {
    runWithA(context(s: String) fun () {})
    runWithA(fun (s: String) {})
    runWithA(fun String.() {})
    runWithA(<!ARGUMENT_TYPE_MISMATCH!>fun () {}<!>)

    runWithA {}
    runWithA <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>s<!> -> }<!>
    runWithA <!ARGUMENT_TYPE_MISMATCH!>{ s: String -> }<!>
}