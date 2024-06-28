// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect annotation class Measurement(val iterations: Int = -1)
expect value class Inline(val value: String = "")


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual annotation class Measurement(actual val iterations: Int)
@JvmInline actual value class Inline(val value: String)

@Measurement()
fun test() {
    Inline() // KT-60476 Fixed in K2
}
