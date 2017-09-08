// "Add data modifier to Foo" "true"
// WITH_RUNTIME
class Foo(val bar: String, val baz: Int)

fun test4() {
    val list = listOf(Foo("A", 1))
    for ((foo, bar) in list<caret>) {
    }
}