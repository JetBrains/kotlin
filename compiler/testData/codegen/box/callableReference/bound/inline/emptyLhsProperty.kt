class X {
    val result: String
        inline get() = "OK"

    fun x(): String {
        return go(::result)
    }
}

inline fun go(f: () -> String): String = f()

fun box(): String {
    return X().x()
}
