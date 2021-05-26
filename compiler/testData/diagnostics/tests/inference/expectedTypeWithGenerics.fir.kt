// !LANGUAGE: +ExpectedTypeFromCast

class X<S> {
    fun <T : S> foo(): T = TODO()
}

fun test(x: X<Number>) {
    val y = x.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as Int
}

fun <S, D: S> g() {
    fun <T : S> foo(): T = TODO()

    val y = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as Int

    val y2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as D
}
