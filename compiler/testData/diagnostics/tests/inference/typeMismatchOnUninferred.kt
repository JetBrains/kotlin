// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
fun <T1> foo22(x: T1 & Any) {}

fun <T> bar(x: T & Any) {
    val z: T = x
    foo22(<!TYPE_MISMATCH!>z<!>)
}