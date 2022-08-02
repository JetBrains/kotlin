// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT
fun test() {
    foo(
        flow { emit(0) }
    ) { it.collect <!TOO_MANY_ARGUMENTS!>{}<!> }
}

fun <F : Any> foo(
    bar: F,
    block: Scope<F>.(F) -> Unit
) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <W> flow(BuilderInference block: FlowCollector<W>.()->Unit): Flow<W> {
    val collector = FlowCollectorImpl<W>()
    collector.block()
    return object : Flow<W> {
        override fun collect(collector: FlowCollector<W>) {
        }
    }
}

class Scope<S>

interface Flow<out O> {
    fun collect(collector: FlowCollector<O>)
}

fun interface FlowCollector<in I> {

    fun emit(value: I)
}

class FlowCollectorImpl<C> : FlowCollector<C> {
    override fun emit(value: C) {}
}

fun Flow<*>.collect() {}
