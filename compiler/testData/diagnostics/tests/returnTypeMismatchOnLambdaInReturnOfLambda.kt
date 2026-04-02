// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun <T> runLike(block: () -> T): T = block()

fun test() {
    var str: String = runLike {
        <!RETURN_TYPE_MISMATCH!>{ }<!>
    }
    str = runLike {
        <!RETURN_TYPE_MISMATCH!>{ it: String -> it }<!>
    }
    str = runLike {
        <!RETURN_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> it }<!>
    }
    str = runLike<String> {
        <!RETURN_TYPE_MISMATCH!>{ }<!>
    }
    str = runLike<String> {
        <!RETURN_TYPE_MISMATCH!>{ it: String -> it }<!>
    }
    str = runLike<String> {
        <!RETURN_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> it }<!>
    }
    str = runLike {
        runLike { <!RETURN_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>{ }<!> }
    }
    str = runLike<String> {
        runLike<String> { <!RETURN_TYPE_MISMATCH!>{ }<!> }
    }
}

/* GENERATED_FIR_TAGS: functionalType, lambdaLiteral, nullableType, typeParameter */
