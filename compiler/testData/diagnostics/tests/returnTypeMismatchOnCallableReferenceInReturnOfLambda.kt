// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun <T> runLike(block: () -> T): T = block()

fun callable(): String = ""

fun <T> genericCallable(): T = null!!

fun test() {
    var str: String = <!TYPE_MISMATCH!>runLike {
        <!TYPE_MISMATCH!>::callable<!>
    }<!>
    str = runLike {
        <!TYPE_MISMATCH!>::genericCallable<!>
    }
    str = runLike<String> {
        <!TYPE_MISMATCH!>::callable<!>
    }
    str = runLike<String> {
        <!TYPE_MISMATCH!>::genericCallable<!>
    }
    runLike<String> {
        <!TYPE_MISMATCH!>::callable<!>
    }
    runLike<String> {
        <!TYPE_MISMATCH!>::genericCallable<!>
    }
}

/* GENERATED_FIR_TAGS: functionalType, lambdaLiteral, nullableType, typeParameter */
