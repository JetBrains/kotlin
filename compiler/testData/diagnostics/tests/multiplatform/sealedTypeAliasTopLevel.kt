// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect sealed class Presence
expect object Online: Presence
expect object Offline: Presence


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Presence = P
sealed class P
actual object Online : P()
actual object Offline : P()
