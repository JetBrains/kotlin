inline fun foo(crossinline x: () -> Unit) = object {
    fun run() {
        bar()
        x()
    }
}.run()

inline fun barNonThrowing() {
    null
}

inline fun bar() {
    null!!
}
