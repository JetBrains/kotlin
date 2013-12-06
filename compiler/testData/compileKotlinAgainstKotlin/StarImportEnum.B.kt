import aaa.E.*

fun main(args: Array<String>) {
    if (TRIVIAL_ENTRY == SUBCLASS) throw AssertionError()
    if (Nested().fortyTwo() != 42) throw AssertionError()
}
