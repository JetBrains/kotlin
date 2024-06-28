// TARGET_BACKEND: NATIVE

// FILE: 1.kt

val O = if (true) "O" else "F" // to avoid const init
val K = if (true) "K" else "A" // to avoid const init

// FILE: main.kt

import kotlin.native.concurrent.*
import kotlin.concurrent.AtomicInt

val sem = AtomicInt(0)

fun box() : String {
    val w1 = Worker.start()
    val w2 = Worker.start()
    val f1 = w1.execute(
        mode = TransferMode.SAFE,
        { },
        {
            sem.incrementAndGet();
            while (sem.value != 3) {}
            O
        }
    )
    val f2 = w2.execute(
        mode = TransferMode.SAFE,
        { },
        {
            sem.incrementAndGet();
            while (sem.value != 3) {}
            K
        }
    )
    while (sem.value != 2) {}
    sem.value = 3
    val result = f1.result + f2.result
    w1.requestTermination().result
    w2.requestTermination().result
    return result
}
