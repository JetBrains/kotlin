package foo

class Foo {
    fun test() {
        <expr>foo.myRun {
            42
        }</expr>
    }
}

inline fun <R> myRun(block: () -> R): R = block()

inline fun <T, R> T.myRun(block: T.() -> R): R = block()
