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

// FILE: a3.kt
// BINARY_ROOT: b3
package library

class T1

typealias A = T1

// FILE: a4.kt
// BINARY_ROOT: b4
package library

class T2

typealias A = T2

// MODULE: main(library)
// FILE: main.kt
package test

class A {
    val unrelated: Int = 0
}

// class_like: library/A
