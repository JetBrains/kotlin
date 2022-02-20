// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect enum class Foo { A, B }
expect enum class Bar { X, Y, Z }

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual enum class Foo { B, A }
actual enum class Bar { X, Z, Y }
