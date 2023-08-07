class Foo {
    fun String.test() {
        var x: Int

        fun call(a: Int) {
            consume(a)
            consume(this@Foo)
            consume(this@test)
            x = 42
        }

        <caret>call(1)
    }
}

fun consume(obj: Any) {}