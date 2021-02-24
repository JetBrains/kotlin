inline fun foo(crossinline x: () -> Unit) = object {
    fun run() {
        barNonThrowing()
        x()
    }
}.run()

inline fun barNonThrowing() {
    null
}

inline fun bar() {
    null!!
}
