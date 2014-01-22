package to

class C {
}

val c = 1

fun g() {
}

fun C.ext() {
}

fun f() {
    a.c
    a.g()
    a.C()
    a.C().ext()
}