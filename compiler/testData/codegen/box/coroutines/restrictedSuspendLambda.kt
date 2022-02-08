// WITH_STDLIB
// TARGET_BACKEND: JVM

import kotlin.coroutines.*

@RestrictsSuspension
interface Marker {
    fun restricted()
}

var lambda: Any? = null

fun acceptsRestricted(c: suspend Marker.() -> Unit) {
    lambda = c
}

fun box(): String {
    acceptsRestricted {}
    @Suppress("INVISIBLE_REFERENCE")
    return if (lambda is kotlin.coroutines.jvm.internal.RestrictedSuspendLambda) "OK"
    else "FAIL"
}