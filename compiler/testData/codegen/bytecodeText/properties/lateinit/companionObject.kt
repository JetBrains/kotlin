// IGNORE_BACKEND: JVM_IR
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

// There's 1 assertion in Foo.Companion.getX, and 4 in Foo.test2 (see KT-28331)
// 5 throwUninitializedPropertyAccessException
