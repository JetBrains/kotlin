// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

class A {
    val one: Int = 1
}

class B

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

class A {
    val two: Int = 2
}

object C

// MODULE: main(library1, library2)
// FILE: main.kt
package test

class A {
    val unrelated: Int = 0
}

// class: library/A
