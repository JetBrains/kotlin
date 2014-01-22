package a

// ALLOW_UNRESOLVED

class C {
}

val c = 1

fun g() {
}

fun C.ext() {
}

<selection>fun f() {
    c
    g()
    C()
    C().ext()
}</selection>