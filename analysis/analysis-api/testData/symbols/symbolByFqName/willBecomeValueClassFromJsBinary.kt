// class: pack/WillBecomeValueClassFromJsBinary
// TARGET_PLATFORM: JS

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
class WillBecomeValueClassFromJsBinary(val value: Int) {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueClassFromJsBinary && value == other.value
    override fun hashCode(): Int = value
    override fun toString(): String = "WillBecomeValueClassFromJsBinary($value)"
}

// MODULE: main(library)
// FILE: main.kt
fun consume(value: pack.WillBecomeValueClassFromJsBinary) {}
