// FIR_IDENTICAL

fun outer() {
    abstract class ALocal {
        abstract fun afun()
        abstract val aval: Int
        abstract var avar: Int
    }

    class Local : ALocal() {
        override fun afun() {}
        override val aval = 1
        override var avar = 2
    }
}
