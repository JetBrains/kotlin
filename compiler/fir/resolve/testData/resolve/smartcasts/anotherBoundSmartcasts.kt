interface A {
    fun foo(): Int

    val x: Int

    fun bar()
}

fun test_1(a: A?) {
    val x = a?.x
    if (x != null) {
        a.bar()
    }
}

fun test_2(a: A?) {
    val x = a?.foo()
    if (x != null) {
        a.bar()
    }
}