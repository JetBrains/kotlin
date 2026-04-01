// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for (<expr>(first, second,)</expr> in x) {}
}
