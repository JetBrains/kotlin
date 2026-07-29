// MODULE: lib
// MODULE_KIND: LibraryBinary
// LANGUAGE: +StrictEquals
// FILE: Lib.kt
class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

// MODULE: main(lib)
// FILE: main.kt
// function: /Base.equals(other)
