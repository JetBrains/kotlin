// WITH_RUNTIME
// FULL_JDK

fun box(): String {
    val x = gen().joinToString()
    if (x != "1, 2") return "fail1: $x"

    val y = gen().joinToString()
    if (y != "-1") return "fail2: $y"
    return "OK"
}

var was = false

fun gen() = generate<Int> {
    if (was) {
        yield(-1)
        return@generate
    }
    for (i in 1..2) {
        yield(i)
    }
    was = true
}

// LIBRARY CODE
fun <T> generate(coroutine c: GeneratorController<T>.() -> Continuation<Unit>): Sequence<T> = object : Sequence<T> {
    override fun iterator(): Iterator<T> {
        val iterator = GeneratorController<T>()
        iterator.setNextStep(c(iterator))
        return iterator
    }
}

class GeneratorController<T>() : AbstractIterator<T>() {
    private lateinit var nextStep: Continuation<Unit>

    override fun computeNext() {
        nextStep.resume(Unit)
    }

    fun setNextStep(step: Continuation<Unit>) {
        this.nextStep = step
    }

    suspend fun yield(value: T): Unit = suspendWithCurrentContinuation { c ->
        setNext(value)
        setNextStep(c)
    }

    operator fun handleResult(result: Unit, c: Continuation<Nothing>) {
        done()
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}
