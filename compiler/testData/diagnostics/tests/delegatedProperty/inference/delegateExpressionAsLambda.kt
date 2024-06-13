// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

import kotlin.reflect.KProperty

fun test(i: Int) {
    val bad by myLazyDelegate {
        createSample(i) { it.toString() }
    }

    takeSample(bad)
}

fun <T> myLazyDelegate(i: () -> T): LazyDelegate<T> = LazyDelegate(i())

class LazyDelegate<T>(val v: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = TODO()
}

class Sample<K, V>

fun takeSample(g: Sample<Int, String>) {}
fun <T, S> createSample(i: T, a: (T) -> S): Sample<T, S> = TODO()