class Q<TT> {
    fun <T> qz(x: T, block: (T) -> String) = block(x)

    fun problematic(): String {
        class CC

        return qz(CC::class) { "OK" }
    }
}

fun box() = Q<Int>().problematic()
