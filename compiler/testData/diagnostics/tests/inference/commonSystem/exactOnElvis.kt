// FIR_IDENTICAL
// SKIP_TXT
interface SelectBuilder<in X> {
    fun foo(block: () -> X)
}

fun <R> select1(builder: SelectBuilder<R>.() -> Unit): R = TODO()
fun <F> select2(builder: SelectBuilder<F>.() -> Unit): F = TODO()
fun <Q> myRun(builder: () -> Q): Q = TODO()

fun <H> bar(w1: H?, w2: H?) {
    val h1: H = myRun {
        select1 {
            foo { w1 }
        } ?: select2 {
            foo { w2 }
        } ?: throw RuntimeException()
    }

    val h2: H = try {
        select1 {
            foo { w1 }
        } ?: select2 {
            foo { w2 }
        } ?: throw RuntimeException()
    } catch (t: Throwable) {
        throw RuntimeException()
    }
}
