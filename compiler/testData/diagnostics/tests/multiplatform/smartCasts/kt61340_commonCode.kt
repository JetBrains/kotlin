// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// MODULE: m1-common
// FILE: common.kt

expect val foo: Any

expect class Bar() {
    val bus: Any
}

fun common() {
    if (foo is String) foo.length

    val bar = Bar()
    if (bar.bus is String) <!SMARTCAST_IMPOSSIBLE{JVM}!>bar.bus<!>.length
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual val foo: Any = 2

actual class Bar actual constructor() {
    actual val bus: Any
        get() = "bus"
}
