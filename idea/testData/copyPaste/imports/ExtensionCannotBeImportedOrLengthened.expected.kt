package to

import a.A
import a.ext
import a.p

fun A.ext() {
}

var A.p: Int
    get() = 2
    set(i: Int) = throw UnsupportedOperationException()

fun A.f() {
    ext()
    p
    p = 3
}