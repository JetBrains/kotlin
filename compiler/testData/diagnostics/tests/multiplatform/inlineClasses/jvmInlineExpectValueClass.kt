// FIR_IDENTICAL
// SKIP_TXT
// MODULE: m1-common
// FILE: common.kt

package kotlin.jvm

annotation class JvmInline

expect value class VC(val a: Any)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

package kotlin.jvm

@JvmInline
actual value class VC(val a: Any)
