// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: a1.kt
// BINARY_ROOT: b1
package library

class A {
    val one: Int = 1
}

class B

// FILE: a2.kt
// BINARY_ROOT: b2
package library

class A {
    val two: Int = 2
}

object C

// MODULE: main(library)
// FILE: main.kt
package test

class A {
    val unrelated: Int = 0
}

// class: library/A
