package test

object X {
    private fun f() { }

    internal inline fun g(x: () -> Unit) {
        x()
        f()
    }
}
