package to

import a.A
import a.ext
import a.infix
import a.p
import a.plus
import a.unaryMinus

fun A.ext() {
}

infix fun A.infix(a: A) {
}

operator fun A.plus(a: A) {
}

operator fun A.unaryMinus() {
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