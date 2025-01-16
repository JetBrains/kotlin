// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74474

fun foo(x: () -> String) {}
fun main(a: Array<String>) {
    foo <!TYPE_MISMATCH!>{
        <!EXPECTED_TYPE_MISMATCH!>a[0] = ""<!>
    }<!>
}
