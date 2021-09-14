class A {
    val a = run {
        class X()

        val y = 10
    }
}

inline fun <R> run(block: () -> R): R {
    return block()
}
