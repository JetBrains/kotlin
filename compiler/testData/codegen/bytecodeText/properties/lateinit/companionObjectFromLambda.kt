class Foo {
    private companion object {
        lateinit var x: String

        fun test() {
            consume({ x }.let { it() });
            { consume(x) }.let { it() }
        }
    }

    fun test2() {
        consume({ x }.let { it() });
        { consume(x) }.let { it() }
    }
}

fun consume(s: String) {}

// There's only one assertion in Foo.Companion.getX
// 1 throwUninitializedPropertyAccessException
