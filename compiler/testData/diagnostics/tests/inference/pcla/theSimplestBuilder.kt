// FIR_IDENTICAL
interface Box<F> {
    fun add(f: F)
}

fun <E> myBuilder(x: Box<E>.() -> Unit) {}

fun <T, R> T.myRun(block: T.() -> R): R = TODO()

fun String.foo(result: String?) {
    //result ?: myRun { Unit }

    myBuilder {

        // K1: OK
        // K2: Argument type mismatch: actual type is 'kotlin.Unit', but 'kotlin.String' was expected.
        result ?: myRun { Unit }

        add("a")

    }
}
