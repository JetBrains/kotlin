// MODULE: m1-common
// FILE: common.kt

expect fun interface F1 {
    fun run()
}

expect fun interface F2 {
    fun run()
}

expect fun interface F3 {
    fun run()
}

expect interface F4 {
    fun run()
}

expect fun interface F5 {
    fun run()
}

expect fun interface F6 {
    fun run()
}

// MODULE: m2-jvm()()(m1-common)

actual fun interface F1 {
    actual fun run()
}

<!ACTUAL_WITHOUT_EXPECT!>actual<!> interface F2 {
    actual fun run()
}

actual typealias F3 = java.lang.Runnable

actual fun interface F4 {
    actual fun run()
}

fun interface F5Typealias {
    fun run()
}

actual typealias F5 = F5Typealias

interface F6Typealias {
    fun run()
}

<!ACTUAL_WITHOUT_EXPECT!>actual<!> typealias F6 = F6Typealias