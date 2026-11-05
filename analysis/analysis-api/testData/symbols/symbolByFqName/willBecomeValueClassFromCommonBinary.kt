// class: pack/WillBecomeValueClassFromCommonBinary
// TARGET_PLATFORM: Common

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
class WillBecomeValueClassFromCommonBinary(val value: Int) {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueClassFromCommonBinary && value == other.value
    override fun hashCode(): Int = value
    override fun toString(): String = "WillBecomeValueClassFromCommonBinary($value)"
}

// MODULE: main(library)
// FILE: main.kt
fun consume(value: pack.WillBecomeValueClassFromCommonBinary) {}
