// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { [<expr>var first</expr>] = x }
}
