class Outer {
    companion object {
        private fun privateMethod() = "OK"
    }

    inner class Inner {
        internal inline fun internalMethod() = privateMethod()
    }
}

fun box(): String {
    return Outer().Inner().internalMethod()
}
