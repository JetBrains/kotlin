// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo(x: Int, y: String) {
    val x: Int
    val y: String
}

expect class Bar(z: Double)

expect class Baz(w: List<String>) {
    val w: List<String>

    operator fun component1(): List<String>

    // Disabled because default arguments are not allowed
    // fun copy(w: List<T> = ...): Baz<T>

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual data class Foo actual constructor(actual val x: Int, actual val y: String)

actual data class Bar actual constructor(val z: Double)

actual data class Baz actual constructor(actual val w: List<String>)
