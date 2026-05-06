// WITH_STDLIB
import kotlin.concurrent.atomics.AtomicArray
import kotlin.concurrent.atomics.AtomicIntArray
import kotlin.concurrent.atomics.AtomicLongArray

@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
class C {
    val ints  = AtomicIntArray(3)
    val longs = AtomicLongArray(3)
    val refs  = AtomicArray(arrayOf("a", "b", "c"))
}
