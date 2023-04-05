class A {
    val it: Number
        field = 4

    fun test() = it + 3

    val p = 5
        get() = field
}

fun test() {
    val c = A().it <!UNRESOLVED_REFERENCE!>+<!> 1
    val d = test()
    val b = A().p + 2
}
