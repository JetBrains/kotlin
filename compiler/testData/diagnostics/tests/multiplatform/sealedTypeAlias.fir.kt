// MODULE: m1-common
// FILE: common.kt

expect sealed class Presence {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>object Online: Presence<!>
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>object Offline: Presence<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Presence = P
sealed class P {
    object Online : P()
    object Offline : P()
}
