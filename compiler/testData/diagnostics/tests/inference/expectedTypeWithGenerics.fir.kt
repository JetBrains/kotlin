// !LANGUAGE: +ExpectedTypeFromCast

class X<S> {
    fun <T : S> foo(): T = TODO()
}

fun test(x: X<Number>) {
    val y = x.foo() as Int
}

fun <S, D: S> g() {
    fun <T : S> foo(): T = TODO()

    val y = <!NEW_INFERENCE_ERROR!>foo()<!> as Int

    val y2 = foo() as D
}
