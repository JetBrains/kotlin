// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// WITH_STDLIB

import java.util.concurrent.atomic.AtomicReference

@JvmInline
value class Box(val name: String)

fun main() {
    val test = Box("Test")
    val rest = Box("Rest")
    val box = AtomicReference(test)

    box.compareAndSet(test, rest)
    println(box.get())
}
