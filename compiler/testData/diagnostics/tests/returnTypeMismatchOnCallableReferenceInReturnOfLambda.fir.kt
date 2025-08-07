// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun <T> runLike(block: () -> T): T = block()

fun callable(): String = ""

fun <T> genericCallable(): T = null!!

fun test() {
    var str: String = runLike {
        ::<!INAPPLICABLE_CANDIDATE!>callable<!>
    }
    str = runLike {
        ::<!INAPPLICABLE_CANDIDATE!>genericCallable<!>
    }
    str = runLike<String> {
        ::<!INAPPLICABLE_CANDIDATE!>callable<!>
    }
    str = runLike<String> {
        ::<!INAPPLICABLE_CANDIDATE!>genericCallable<!>
    }
    runLike<String> {
        ::<!INAPPLICABLE_CANDIDATE!>callable<!>
    }
    runLike<String> {
        ::<!INAPPLICABLE_CANDIDATE!>genericCallable<!>
    }
}

/* GENERATED_FIR_TAGS: functionalType, lambdaLiteral, nullableType, typeParameter */
