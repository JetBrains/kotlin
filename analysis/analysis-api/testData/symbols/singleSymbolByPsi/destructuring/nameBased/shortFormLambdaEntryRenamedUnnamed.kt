// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (<expr>_ = first</expr>, second,) -> }
}
