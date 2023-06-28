// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
interface Shared {
    fun sharedMethod(withDefaultParam: Int = 2) {}
}

interface Transitive

expect class Foo : Transitive

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : Transitive
