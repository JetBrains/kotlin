// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun expanded(maxCapacity: Int, newCapacity: Int, buffer: Array<Any?>) {
    select(buffer.copyOf(newCapacity), toArray(arrayOfNulls(newCapacity)))
}

fun <K> select(x: K, y: K): K = x

fun <T> Array<T>.copyOf(newSize: Int): Array<T?> = TODO()
inline fun <reified V> arrayOfNulls(size: Int): Array<V?> = TODO()
fun <S> toArray(array: Array<S>): Array<S> = TODO()
