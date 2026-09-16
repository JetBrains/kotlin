class Foo {
    private companion object {
        lateinit var x: String

        fun test() {
            consume(x)
            consume(x)
            consume(x)
            consume(x)
        }
    }

    fun test2() {
        consume(x)
        consume(x)
        consume(x)
        consume(x)
    }
}

fun consume(s: String) {}

// JVM backend invokes getX() (as suggested in KT-28331)
// 1 throwUninitializedPropertyAccessException
