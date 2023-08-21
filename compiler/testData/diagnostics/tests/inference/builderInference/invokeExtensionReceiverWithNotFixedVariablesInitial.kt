// FIR_IDENTICAL

interface Controller<F> {
    fun yield(t: F)
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface Topic<F>

fun <E : Any> subscribe(topic: Topic<E>, handler: E) {}

fun <L : Any, K> messageBusFlow(
    topic: Topic<L>,
    initialValue: (suspend () -> K)? = null,
    listener: suspend Controller<K>.() -> L
) {
    generate {
        initialValue?.let { yield(it()) }
        subscribe(topic, listener())
    }
}