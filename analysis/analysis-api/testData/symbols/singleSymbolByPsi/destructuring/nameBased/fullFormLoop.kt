// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for (<expr>(val first, val second,)</expr> in x) {}
}
