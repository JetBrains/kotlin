// TARGET_BACKEND: JVM
// WITH_STDLIB
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// IGNORE_BACKEND: ANDROID
// Android test generator always runs IR validator

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
