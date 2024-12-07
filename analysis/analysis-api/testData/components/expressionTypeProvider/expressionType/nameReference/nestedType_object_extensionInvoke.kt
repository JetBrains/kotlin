class Foo {
    object Bar
}

operator fun Bar.invoke() {}

fun foo() {
    Foo.<expr>Bar</expr>()
}