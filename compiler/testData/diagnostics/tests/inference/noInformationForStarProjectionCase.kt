// ISSUE: KT-56134
// WITH_STDLIB

class X {
    fun foo(ls: List<*>) {}
}

fun main() {
    val x = X().foo(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>mutableListOf<!>()
    )
}
