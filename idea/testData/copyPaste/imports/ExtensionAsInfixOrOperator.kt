package a

class A() {}

fun A.plus(a: A) = this

fun A.infix(i: Int) = i

<selection>fun f(a: A) {
    a + a
    a infix 1
}</selection>