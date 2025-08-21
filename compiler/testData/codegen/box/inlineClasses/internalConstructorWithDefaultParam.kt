// ISSUE: KT-74470

// TARGET_BACKEND: JVM
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// WITH_STDLIB


// MODULE: lib
// FILE: lib.kt

@JvmInline
value class ValueClass internal constructor(val i: Int = 0) {
    fun foo() = "OK"
}

// MODULE: main(lib)
// FILE: main.kt

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun box() = ValueClass().foo()