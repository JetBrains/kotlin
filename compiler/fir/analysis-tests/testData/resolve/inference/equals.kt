// ISSUE: KT-47409
fun <T> materialize(): T = TODO()

fun main() {
    if ("" == materialize()) return // FE1.0: OK, type argument inferred to Any?
    if (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() == "") return // FE1.0: Error, uninferred type argument for `T`
}
