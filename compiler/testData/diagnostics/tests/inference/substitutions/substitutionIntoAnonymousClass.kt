class Test {
    private fun <T : Any> T.self() = object {
        fun bar(): T {
            return this@self
        }
    }
    fun test() {
        1.self().bar() + 1
    }
}

class Foo<R> {
    private fun <T> bar() = object {
        fun baz(): Foo<R> {
            return this@Foo
        }
    }

    fun getR(r: R) = r

    fun test() {
        Foo<Int>().bar<String>().baz().getR(1)
        Foo<Int>().bar<String>().baz().getR(<!TYPE_MISMATCH!>""<!>)
    }
}