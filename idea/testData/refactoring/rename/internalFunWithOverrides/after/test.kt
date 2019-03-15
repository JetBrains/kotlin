sealed class X {
    internal abstract fun newOverridableMethod(x: Int): Int

    abstract class Y : X() {
        override fun newOverridableMethod(x: Int): Int = 1
    }

    class Z : Y() {
        override fun newOverridableMethod(x: Int): Int =
                if (x > 0) x
                else super.newOverridableMethod(x)
    }
}

fun get() : X? {
    return X.Z()
}

fun test() {
    get()?.newOverridableMethod(2)
}
