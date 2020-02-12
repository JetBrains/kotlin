object Foo {
    operator fun <T> invoke() {}
}

fun main() {
    Foo<Int>()
}