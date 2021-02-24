// !DIAGNOSTICS: -UNUSED_VARIABLE

class Outer<T> (val v: T) {
    val prop: Any?

    init {
        class Inner(val v: T) {
            override fun toString() = v.toString()
        }

        val value: Inner = Inner(v)
        prop = value
    }
}

fun box(): String {
    return Outer("OK").prop.toString()
}
