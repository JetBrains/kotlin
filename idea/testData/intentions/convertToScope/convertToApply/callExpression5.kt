// WITH_RUNTIME

class Foo {
    fun foo(i: Int) {}

    fun test(f: Foo) {
        val f = Foo()<caret>
        f.foo(1)
        f.foo(2)
        bar(2, this)
    }
}

fun bar(i: Int, f: Foo) {}


