// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) {
        var (<expr>first</expr>, second,) = x
    }
}
