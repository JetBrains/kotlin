// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
val x = object<T> {
    fun foo(t: T) {}
}

fun <R> test() {
    val y = object<T : R> {
        fun foo(t: T) {}
    }
}
