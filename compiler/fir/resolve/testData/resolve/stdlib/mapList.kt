fun main() {
    val x = emptyList<Int>()

    val u = x.map { it + it }
    u.applyX {
        1 in this
        contains(1)
    }

}


inline fun <T> T.applyX(block: @ExtensionFunctionType Function1<T, Unit>): T {
    TODO()
}