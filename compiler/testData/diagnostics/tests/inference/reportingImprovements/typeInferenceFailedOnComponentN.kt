// !DIAGNOSTICS: -UNUSED_VARIABLE

class X

operator fun <T> X.component1(): T = TODO()

fun test() {
    val (y) = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>X()<!>
}