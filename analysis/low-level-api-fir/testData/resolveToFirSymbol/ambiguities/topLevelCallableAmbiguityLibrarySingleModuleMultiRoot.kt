// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: a1.kt
// BINARY_ROOT: b1
package library

class R1

fun callable(): R1 = R1()

fun unrelated(): Int = 0

// FILE: a2.kt
// BINARY_ROOT: b2
package library

class R2

fun callable(): R2 = R2()

fun unrelated(): Int = 0

// FILE: a3.kt
// BINARY_ROOT: b3
package library

class R3

val callable: R3 = R3()

val unrelated: Int = 0

// FILE: a4.kt
// BINARY_ROOT: b4
package library

class R4

val callable: R4 = R4()

val unrelated: Int = 0

// MODULE: main(library)
// FILE: main.kt
package test

class Unrelated

fun callable(): Unrelated = Unrelated()

val callable: Unrelated = Unrelated()

// callable: library/callable
