// !DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> materialize(): T = TODO()

fun main() {
    val x = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!> { <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() }
}
