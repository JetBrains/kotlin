// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FILE: mySynchronized.kt

import kotlin.jvm.internal.unsafe.*

public inline fun <R> mySynchronized(lock: Any, block: () -> R): R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
        monitorExit(lock)
    }
}

// FILE: box.kt
fun box() = mySynchronized(Any()) { "OK" }