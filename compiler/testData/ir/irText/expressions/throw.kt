fun test1() {
    throw Throwable()
}

fun testImplicitCast(a: Any) {
    if (a is Throwable) {
        throw a
    }
}
