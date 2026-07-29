// LANGUAGE: +StrictEquals

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
open class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

// MODULE: main(lib)
// FILE: main.kt
class Derived : Base() {
    override fun equ<caret>als(other: Any?): Boolean = true
}
