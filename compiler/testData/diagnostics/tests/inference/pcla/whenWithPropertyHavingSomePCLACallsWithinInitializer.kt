// FIR_IDENTICAL

interface Controller<F> {
    fun yield(t: F)
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(b: Boolean, buffer: Collection<String>) {
    generate {
        if (b) {
            val x: String = myLaunch {
                myRun {
                    yield(buffer.toTypedArray2())
                }
            }
        }
    }.get(0).length
}

fun <E> Collection<E>.toTypedArray2(): Array<E> = TODO()

fun myLaunch(
    block: () -> Unit
): String = TODO()

fun myRun(action: () -> Unit): Unit = TODO()