// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

class L1

fun callable(): L1 = L1()

fun unrelated(): Int = 0

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

class L2

fun callable(): L2 = L2()

fun unrelated(): Int = 0

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: library3.kt
package library

class L3

val callable: L3 = L3()

val unrelated: Int = 0

// MODULE: library4
// MODULE_KIND: LibraryBinary
// FILE: library4.kt
package library

class L4

val callable: L4 = L4()

val unrelated: Int = 0

// MODULE: main(library1, library2, library3, library4)
// FILE: main.kt
package test

class Unrelated

fun callable(): Unrelated = Unrelated()

val callable: Unrelated = Unrelated()

// callable: library/callable
