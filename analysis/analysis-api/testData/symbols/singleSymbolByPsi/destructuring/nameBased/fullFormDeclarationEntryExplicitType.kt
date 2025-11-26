// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) {
        (<expr>val first: String</expr>, var second,) = x
    }
}
