// WITH_RUNTIME

class Foo {
    fun foo(i: Int) {}
}

fun bar(i: Int, f: Foo) {}

fun test() {
    val f2 = Foo()
    val f = Foo()<caret>
    f.foo(1)
    bar(2, f)
    bar(3, f2)
}