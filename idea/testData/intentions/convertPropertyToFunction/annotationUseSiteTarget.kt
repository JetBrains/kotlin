annotation class A
annotation class B

interface Foo {
    @get:A
    @B
    val <caret>bar: String
}