// IGNORE_BACKEND: JVM
// See KT-38833: Runtime exception is "java.lang.ClassCastException: java.lang.Integer cannot be cast to kotlin.UInt"
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    var result = 0u
    for (i: UInt? in 1u..3u) {
        result = sum(result, i)
    }
    return if (result == 6u) "OK" else "fail: $result"
}

fun sum(i: UInt, z: UInt?): UInt {
    return i + z!!
}
