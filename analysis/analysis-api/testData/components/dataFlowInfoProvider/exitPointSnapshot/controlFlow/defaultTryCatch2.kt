// WITH_STDLIB

fun test() {
    consume(1)
    <expr>try {
        dangerous()
    } catch (e: FooException) {
        consume(e.message?.length ?: 0)
        throw e
    }</expr>
}

fun consume(n: Int) {}

@Throws(FooException::class)
fun dangerous(): String {
    return "foo"
}

class FooException : Exception()