import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.test.*

fun main() {
    kotlin.native.internal.GC.cyclicCollectorEnabled = true

    repeat(10000) {
        // Create atomic cyclic garbage:
        val ref = AtomicReference<Any?>(null)
        ref.value = ref
    }

    // main thread will then run cycle collector termination, which involves running it and cleaning everything up.
    // 10000 references should hit [kGcThreshold] then.
}