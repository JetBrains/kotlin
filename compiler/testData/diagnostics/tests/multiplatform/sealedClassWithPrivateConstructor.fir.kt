// ISSUE: KT-58033
// MODULE: m1-common
// FILE: common.kt

expect sealed class Frame <!EXPECTED_PRIVATE_CONSTRUCTOR_WARNING!>private<!> constructor()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual sealed class Frame actual constructor()
