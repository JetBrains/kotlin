// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for (<expr>[val first, val second,]</expr> in x) {}
}
