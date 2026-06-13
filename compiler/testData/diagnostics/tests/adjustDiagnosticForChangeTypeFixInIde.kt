// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75874

// "Change type from 'String' to '(Int) -> String'" "true"
fun foo(param: ((Int) -> String) -> String) {
    foo {
        <!EXPECTED_PARAMETER_TYPE_MISMATCH!>f: String<!> -> <!UNRESOLVED_REFERENCE!>f<!>(42)
    }
}

fun bar(param: ((Int) -> String, (Boolean) -> String) -> String) {
    bar {
        <!EXPECTED_PARAMETER_TYPE_MISMATCH!>f: String<!>, <!EXPECTED_PARAMETER_TYPE_MISMATCH!>g: Boolean<!> -> <!UNRESOLVED_REFERENCE!>f<!>(42, 20)
    }
}

abstract class MyCustomFunction : ((Int) -> String) -> String

fun baz(param: MyCustomFunction) {
    baz <!ARGUMENT_TYPE_MISMATCH!>{
        f: String -> <!UNRESOLVED_REFERENCE!>f<!>(42)
    }<!>
}

fun interface MySamFunction : ((Int) -> String) -> String

fun fuz(param: MySamFunction) {
    fuz {
        <!EXPECTED_PARAMETER_TYPE_MISMATCH!>f: String<!> -> <!UNRESOLVED_REFERENCE!>f<!>(42)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral */
