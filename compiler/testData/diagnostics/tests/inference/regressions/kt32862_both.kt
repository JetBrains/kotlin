// DIAGNOSTICS: -UNUSED_PARAMETER

interface G<T> {
    fun build(): G<T>
}
class V1<V>(val value: V)
class V2<V>(val value: V)
fun <V, T : V?> G<T>.foo(vararg values: V1<V>) = build()
fun <V, T : V?> G<T>.foo(vararg values: V2<V?>) = build()

fun forReference(ref: Any?) {}

fun test() {
    forReference(G<Int?>::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>foo<!>)
}
