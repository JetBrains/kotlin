package runtime.workers.enum_identity

import kotlin.test.*
import konan.worker.*

enum class A {
    A, B
}

data class Foo(val kind: A)

// Enums are shared between threads so identity should be kept.
@Test
fun runTest() {
    val result = startWorker().schedule(TransferMode.CHECKED, { Foo(A.B) }, { input ->
        input.kind == A.B
    }).result()
    println(result)
}