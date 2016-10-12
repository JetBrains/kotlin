// WITH_RUNTIME
class Foo(var bar: Int)

fun test() {
    val foo = Foo(1)
    println(<selection>foo.bar</selection>)
    foo.bar = foo.bar + 1
}