// WITH_STDLIB

abstract class BaseGeneric<T>(val t: T) {
    abstract fun iterate()
}

class Derived(array: DoubleArray) : BaseGeneric<DoubleArray>(array) {
    var test = 0

    override fun iterate() {
        test = 0
        for (i in t.indices) {
            test = test * 10 + (i + 1)
        }
    }
}

fun box(): String {
    val t = Derived(doubleArrayOf(0.0, 0.0, 0.0, 0.0))
    t.iterate()
    return if (t.test == 1234) "OK" else "Fail: ${t.test}"
}
