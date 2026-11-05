// class: pack/WillBecomeValueClassFromBinary

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
class WillBecomeValueClassFromBinary(val value: Int) {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueClassFromBinary && value == other.value
    override fun hashCode(): Int = value
    override fun toString(): String = "WillBecomeValueClassFromBinary($value)"
}

// MODULE: main(library)
// FILE: main.kt
fun consume(value: pack.WillBecomeValueClassFromBinary) {}
