// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

class X<S> {
    fun <T : S> foo(): T = TODO()
}

fun test(x: X<Number>) {
    val <!UNUSED_VARIABLE!>y<!> = x.foo() as Int
}

fun <S, D: S> g() {
    fun <T : S> foo(): T = TODO()

    val <!UNUSED_VARIABLE!>y<!> = <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo<!>() as Int

    val <!UNUSED_VARIABLE!>y2<!> = foo() as D
}