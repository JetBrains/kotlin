fun test() {
    val x = intArrayOf(1, 2, 3)
    x[1] = 0
}

fun foo() = 1

fun test2() {
    intArrayOf(1, 2, 3)[foo()] = 1
}