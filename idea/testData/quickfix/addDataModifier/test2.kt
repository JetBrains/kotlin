// "Make 'Foo' data class" "true"
class Foo(val bar: String, var baz: Int)

fun test() {
    val foo = Foo("A", 1)
    var (bar, baz) = foo<caret>
}