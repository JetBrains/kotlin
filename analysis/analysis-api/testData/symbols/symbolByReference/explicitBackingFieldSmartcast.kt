
class Foo {
    val value: List<String>
        field: MutableList<String> = mutableListOf()

    fun test() {
        v<caret>alue.add("foo")
    }
}
