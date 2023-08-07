class Foo {
    fun test() {
        fun call() {
            consume(this@Foo)
        }

        <caret>call()
    }
}

fun consume(obj: Foo) {}