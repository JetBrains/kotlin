annotation class Foo<T>(val s: String)

@Foo<Int>("")
fun <caret>foo() {
}