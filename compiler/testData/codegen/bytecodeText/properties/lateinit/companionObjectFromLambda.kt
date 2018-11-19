class Foo {
    private companion object {
        lateinit var x: String

        fun test() {
            consume({ x }());
            { consume(x) }()
        }
    }

    fun test2() {
        consume({ x }());
        { consume(x) }()
    }
}

fun consume(s: String) {}

// There's only one assertion in Foo.Companion.getX
// 1 throwUninitializedPropertyAccessException
