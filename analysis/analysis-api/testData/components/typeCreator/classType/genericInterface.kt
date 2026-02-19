// ARGUMENT: OUT_1

interface A<T> {}
class B: A<Int>

val x = <expr>(B() as A<*>)</expr>

val yy = "he<caret_1>llo"
