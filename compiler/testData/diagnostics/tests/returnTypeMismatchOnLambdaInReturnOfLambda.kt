// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun <T> runLike(block: () -> T): T = block()

fun test() {
    var str: String = <!TYPE_MISMATCH!>runLike {
        <!TYPE_MISMATCH!>{ }<!>
    }<!>
    str = <!TYPE_MISMATCH!>runLike {
        { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>it: String<!> -> <!TYPE_MISMATCH!>it<!> }
    }<!>
    str = runLike {
        <!TYPE_MISMATCH, TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> }<!>
    }
    str = runLike<String> {
        <!TYPE_MISMATCH!>{ }<!>
    }
    str = runLike<String> {
        <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETER_TYPE_MISMATCH!>it: String<!> -> <!TYPE_MISMATCH!>it<!> }<!>
    }
    str = runLike<String> {
        <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> }<!>
    }
    str = <!TYPE_MISMATCH!>runLike {
        runLike { <!TYPE_MISMATCH!>{ }<!> }
    }<!>
    str = runLike<String> {
        runLike<String> { <!TYPE_MISMATCH!>{ }<!> }
    }
}

/* GENERATED_FIR_TAGS: functionalType, lambdaLiteral, nullableType, typeParameter */
