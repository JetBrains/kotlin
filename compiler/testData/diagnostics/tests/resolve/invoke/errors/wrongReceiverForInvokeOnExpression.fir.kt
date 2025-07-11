// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

fun test1() {
    1. <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(fun String.(i: Int) = i )<!>(1)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    11.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(f)<!>(1)
    11.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(f)<!>()
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(foo())<!>(1)
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localFunction, typeWithExtension */
