// DO_NOT_CHECK_SYMBOL_RESTORE_K1

class Foo {
    val value: List<String>
        field: MutableList<String> = mutableListOf()

    fun test() {
        v<caret>alue.add("foo")
    }
}