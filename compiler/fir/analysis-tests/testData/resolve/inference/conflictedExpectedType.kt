// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76781

fun <T : Any> foo(): MutableList<T> = TODO()

fun main() {
    val x: MutableList<String?> = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
}