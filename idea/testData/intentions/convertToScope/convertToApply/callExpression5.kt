// WITH_RUNTIME
// IS_APPLICABLE: false

class Foo {
    fun foo(i: Int) {}

    fun test(f: Foo) {
        val f = Foo()<caret>
        f.foo(1)
        bar(2, this)
    }
}

fun bar(i: Int, f: Foo) {}


