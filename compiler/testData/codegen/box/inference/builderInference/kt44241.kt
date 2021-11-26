// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference
class A
class B<K> {}
class Scope<K11, K22>(
    val key: K11,
) {
    fun emit(key: K22) {}
}

@OptIn(ExperimentalTypeInference::class)
fun <K1, K2> B<K1>.scoped(@kotlin.BuilderInference binder: Scope<K1, K2>.() -> Unit) {}

fun run(x: B<A>) {
    x.scoped { emit(key) }
}

fun box(): String {
    run(B<A>())
    return "OK"
}