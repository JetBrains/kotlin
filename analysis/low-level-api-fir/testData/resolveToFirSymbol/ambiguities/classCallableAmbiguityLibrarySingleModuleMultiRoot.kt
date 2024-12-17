// IGNORE_FIR
// KT-72988

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: a1.kt
// BINARY_ROOT: b1
package library

class R1

class A {
    fun callable(): R1 = R1()
}

class B

// FILE: a2.kt
// BINARY_ROOT: b2
package library

class R2

class A {
    fun callable(): R2 = R2()
}

object C

// FILE: a3.kt
// BINARY_ROOT: b3
package library

class R3

class A {
    val callable: R3 = R3()
}

// FILE: a4.kt
// BINARY_ROOT: b4
package library

class R4

class A {
    val callable: R4 = R4()
}

// MODULE: main(library)
// FILE: main.kt
package test

class Unrelated

class A {
    fun callable(): Unrelated = Unrelated()
}

// callable: library/A.callable
