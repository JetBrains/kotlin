// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

class Foo {
    val value: List<String>
        field: MutableList<String> = mutableListOf()

    fun test() {
        v<caret>alue.add("foo")
    }
}