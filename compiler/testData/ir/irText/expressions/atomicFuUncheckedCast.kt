// FIR_IDENTICAL
class AtomicRef<T>(var value: T)

class Box(val b: Int)

fun test(bs: AtomicRef<Any?>) {
    bs as AtomicRef<Array<Array<Box>>>
    bs.value[0][1]!!.b
}