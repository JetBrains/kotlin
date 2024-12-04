// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-72238

interface Box<F> {
    fun add(f: F)
}

fun <E> myBuilder(x: Box<E>.() -> Unit) {}

fun <T, R> T.myRun(block: T.() -> R): R = TODO()

fun <K : Any> elvisLike(x: K?, y: K): K = TODO()

fun String.foo(result: String?) {
    myBuilder {
        // Should infer to elvisLike<Any>(...)
        elvisLike(result, myRun { Unit })

        add("a")
    }
}
