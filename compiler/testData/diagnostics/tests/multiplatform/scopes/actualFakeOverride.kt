// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class HashMap {
    val size: Int
}

expect abstract class AbstractMap {
    val size: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class AbstractMap() {
    actual val size: Int = 0
}

actual class HashMap : AbstractMap()
