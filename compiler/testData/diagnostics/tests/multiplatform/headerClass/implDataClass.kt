// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo(x: Int, y: String) {
    val x: Int
    val y: String
}

header class Bar(z: Double)

header class Baz(w: List<String>) {
    val w: List<String>

    operator fun component1(): List<String>

    // Disabled because default arguments are not allowed
    // fun copy(w: List<T> = ...): Baz<T>

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl data class Foo(impl val x: Int, impl val y: String)

impl data class Bar(val z: Double)

impl data class Baz(impl val w: List<String>)
