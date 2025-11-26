// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (<expr>val foo = first</expr>, val second,) -> }
}
