object Foo {
    operator fun <T> invoke() {}
}

fun main(args: Array<String>) {
    Foo<Int>()
}