class A {
    val a = myRun {
        class X()

        val y = 10
    }
}

inline fun <R> myRun(block: () -> R): R {
    return block()
}
