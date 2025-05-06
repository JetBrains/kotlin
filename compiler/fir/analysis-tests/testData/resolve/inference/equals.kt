// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47409
fun <T> materialize(): T = TODO()

fun main() {
    if ("" == materialize()) return // FE1.0: OK, type argument inferred to Any?
    if (<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>() == "") return // FE1.0: Error, uninferred type argument for `T`
}
