// WITH_RUNTIME

class Foo {
    companion object {
        fun create(x: String): Foo = Foo()
    }
}

fun main(args: Array<String>) {
    listOf("a").map {<caret> Foo.create(it) }
}