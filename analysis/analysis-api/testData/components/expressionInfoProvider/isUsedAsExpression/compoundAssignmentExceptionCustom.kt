class C {
    operator fun remAssign(other: C) {

    }
}

fun test(): C {
    var n = C()
    <expr>n %= n</expr>
    return n
}