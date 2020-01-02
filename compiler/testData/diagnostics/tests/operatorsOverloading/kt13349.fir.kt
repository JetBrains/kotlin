object Foo {
    operator fun <T> invoke() {}
}

fun main() {
    <!INAPPLICABLE_CANDIDATE!>Foo<!><Int>()
}