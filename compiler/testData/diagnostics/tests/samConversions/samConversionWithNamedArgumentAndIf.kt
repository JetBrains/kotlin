// FIR_IDENTICAL
// ISSUE: KT-67311

fun interface Callable<in T, out R> {
    operator fun invoke(arg: T): R
}

fun<T> findMessages(msg: Callable<T, String?>) {}

fun runTest() {
    findMessages<Int>(
        msg = if (true) {
            { "a" }
        } else {
            { "b" }
        }
    )
}
