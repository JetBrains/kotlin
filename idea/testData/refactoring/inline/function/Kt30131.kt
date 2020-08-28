fun main() {
    val foo<caret> = Foo<List<String>>(listOf())
    foo
}

class Foo<A>(first: A)