// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// WITH_STDLIB

fun test() {
    class Foo {
        val foo: List<String>
            field: MutableList<String> = mutableListOf()
    }
}