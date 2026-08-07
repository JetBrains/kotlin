// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// API_VERSION: 1.9
// DUMP_INFERENCE_LOGS: MARKDOWN
fun test() {
    foo(
        flow { emit(0) }
    ) { it.collect {} }

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

    // 2. inside { it.collect {} }
    // F == Flow<W> / F is fixed on demand to resolve the `it.collect` call, with W left not fixed
    // so `it` is Flow<Int> (once W := Int) and `collect` is resolved
    // Unit <: Unit / from Lambda argument, probably { it.collect {} }
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

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, funInterface,
funWithExtensionReceiver, functionDeclaration, functionalType, in, integerLiteral, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, out, override, propertyDeclaration, starProjection, typeConstraint, typeParameter,
typeWithExtension */
