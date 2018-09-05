// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect sealed class Presence {
    object Online: Presence
    object Offline: Presence
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual typealias Presence = P
sealed class P {
    object Online : P()
    object Offline : P()
}