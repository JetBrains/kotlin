// WITH_RUNTIME
fun test(list: List<String>) {
    Foo().bar().<caret>mapIndexed { index, _ ->
        index + 42
    }
}

class Foo {
    fun bar() = emptyList<String>()
}