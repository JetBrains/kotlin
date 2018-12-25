// WITH_RUNTIME
// IS_APPLICABLE: false

class Foo {
    fun foo(i: Int) {}
}

fun bar(i: Int, f: Foo) {}

fun test() {
    listOf(1).forEach {
        val f = Foo()<caret>
        f.foo(1)
        bar(it, f)
    }
}