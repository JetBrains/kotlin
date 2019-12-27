// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.jvm.internal.unsafe.*

@Suppress("INVISIBLE_MEMBER")
fun box(): String {
    val lock = Any()
    monitorEnter(lock)
    try {
        return "OK"
    }
    finally {
        monitorExit(lock)
    }
}
