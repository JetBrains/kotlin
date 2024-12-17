// IGNORE_FIR
// KT-72988

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: a1.kt
// BINARY_ROOT: b1
package library

enum class A {
    Entry {
        val one: Int = 1
    }
}

enum class B {
    Entry
}

// FILE: a2.kt
// BINARY_ROOT: b2
package library

enum class A {
    Entry {
        val two: Int = 2
    }
}

object C

// MODULE: main(library)
// FILE: main.kt
package test

enum class A {
    Entry {
        val unrelated: Int = 0
    }
}

// callable: library/A.Entry
