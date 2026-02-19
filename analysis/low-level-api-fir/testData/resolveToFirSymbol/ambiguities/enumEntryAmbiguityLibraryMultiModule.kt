// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

enum class A {
    Entry {
        val one: Int = 1
    }
}

enum class B {
    Entry
}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

enum class A {
    Entry {
        val two: Int = 2
    }
}

object C

// MODULE: main(library1, library2)
// FILE: main.kt
package test

enum class A {
    Entry {
        val unrelated: Int = 0
    }
}

// callable: library/A.Entry
