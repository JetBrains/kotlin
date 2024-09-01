// WITH_STDLIB
// SKIP_TXT
fun test() {
    foo(
        flow { emit(0) }
    ) { <!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>.collect <!TOO_MANY_ARGUMENTS!>{}<!> }

    // 0. Initial
    // W <: Any / declared upper bound
    // FlowCollector<W>.() -> Unit <: FlowCollector<W>.() -> Unit / from Argument { emit(0) }
    // F <: Any / declared upper bound
    // Flow<W> <: F / from Argument flow { emit(0) }
    // Scope<F>.(F) -> Unit -> Scope<F>.(F) -> Unit / from Argument { it.collect() }

    // 1. after analyze for { emit(0 }
    // Unit <: Unit / from Lambda argument, probably { emit(0) }
    // Int <: W / from For builder inference call
    // Flow<Int> <: F / from For builder inference call

    // 2. after analyze for { it.collect {} }
    // Unit <: Unit / from Lambda argument, probably { it.collect {} }
    // Flow<*> <: F / from For builder inference call
    // ERROR_TYPE <: W / from For builder inference call
}

fun <F : Any> foo(
    bar: F,
    block: Scope<F>.(F) -> Unit
) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <W> flow(@BuilderInference block: FlowCollector<W>.()->Unit): Flow<W> {
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
