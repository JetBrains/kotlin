// WITH_RUNTIME
class Foo(val bar: () -> Int)

fun bar(foo: Foo?) {
    foo?.<caret>let { it.bar.invoke() }
}