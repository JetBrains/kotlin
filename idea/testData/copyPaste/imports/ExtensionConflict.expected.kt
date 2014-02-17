package to

import a.A

fun A.ext() {
}

fun A.infix(a: A) {
}

fun A.plus(a: A) {
}

fun A.minus() {
}

val A.p: Int
    get() = 2

fun f() {
    A().ext()
    A() + A()
    A() infix A()
    -A()
    A().p
}