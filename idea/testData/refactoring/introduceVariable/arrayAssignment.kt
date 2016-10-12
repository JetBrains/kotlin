// WITH_RUNTIME
class Foo(var bar: IntArray)

fun test() {
    val foo = Foo(IntArray(1) { 1 })
    println(<selection>foo.bar</selection>)
    foo.bar[0] = foo.bar[0] + 1
}