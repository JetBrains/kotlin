// WITH_STDLIB
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED

import kotlin.time.*

fun box(): String {
    TimedValue(0, Duration.ZERO)
    return "OK"
}
