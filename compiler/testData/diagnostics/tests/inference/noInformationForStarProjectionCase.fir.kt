// ISSUE: KT-56134
// WITH_STDLIB

class X {
    fun foo(ls: List<*>) {}
}

fun main() {
    val x = X().foo(
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>mutableListOf<!>()
    )
}
