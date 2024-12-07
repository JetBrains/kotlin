actual class B {
    actual fun bar() {}
    fun actBar() {}
}

fun actualAcceptB(b: B) {
    b.bar()
    b.actBar()
}

fun test() {
    acceptA(A())
    acceptB(B())
    actualAcceptB(B())
}
