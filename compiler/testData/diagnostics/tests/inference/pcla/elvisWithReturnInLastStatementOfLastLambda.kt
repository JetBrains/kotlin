// FIR_IDENTICAL

class Controller<T1> {
    fun yield(t: T1) {}
}

fun <T2> generate(
    block: Controller<T2>.() -> Unit
): T2 = TODO()

class Res<E1> {
    val e: E1 get() = TODO()
}

fun foo(x: String): Res<String> = TODO()

fun <E2> emptyRes(): Res<E2> = TODO()

fun <R> myRun(r: () -> R): R = TODO()

fun baz(x: String?) {
    generate {
        val y = myRun {
            foo(x ?: return@myRun emptyRes())
        }

        yield(y)
    }.e.length
}
