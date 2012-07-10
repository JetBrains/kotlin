//KT-2320 failure of complex case of type inference
package i

trait NotMap<B>

trait Entry<B> {
    fun getValue(): B
}


fun <V, R> NotMap<V>.mapValuesOriginal(<!UNUSED_PARAMETER!>ff<!>: (Entry<V>) -> R): NotMap<R> = throw Exception()

fun <B, C> NotMap<B>.mapValuesOnly(f: (B) -> C) = mapValuesOriginal { e -> f(e.getValue()) }
