// class: pack/WillBecomeValueObjectFromBinary

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
object WillBecomeValueObjectFromBinary {
    override fun toString(): String = "WillBecomeValueObjectFromBinary"
}

// MODULE: main(library)
// FILE: main.kt
fun consume(value: pack.WillBecomeValueObjectFromBinary) {}
