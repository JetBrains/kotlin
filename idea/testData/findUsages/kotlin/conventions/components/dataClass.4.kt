package pack4

import pack.A

fun takeExtFun(p: A.() -> Unit) {
}

fun takeFun1(p: (A) -> Unit) {
}

fun takeFun2(p: ((A?, Int) -> Unit)?) {
}

fun takeFun3(p: (List<A>) -> Unit) {
}

fun takeFuns(p: MutableList<(A) -> Unit>) {
    p[0] = { val (x, y) = it }
}

fun <T> x(p1: T, p2: (T) -> Unit){}

fun foo(p: A) {
    takeExtFun { val (x, y) = this }

    takeFun1 { val (x, y) = it }
    takeFun2 { a, n -> val (x, y) = a!! }
    takeFun3 { val (x, y) = it[0] }

    x(p) { val (x, y) = it }
}
