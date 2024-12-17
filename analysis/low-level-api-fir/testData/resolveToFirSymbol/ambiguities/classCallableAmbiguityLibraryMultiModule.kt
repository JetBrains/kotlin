// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

class L1

class A {
    fun callable(): L1 = L1()
}

class B

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

class L2

class A {
    fun callable(): L2 = L2()
}

object C

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: library3.kt
package library

class L3

class A {
    val callable: L3 = L3()
}

// MODULE: library4
// MODULE_KIND: LibraryBinary
// FILE: library4.kt
package library

class L4

class A {
    val callable: L4 = L4()
}

// MODULE: main(library1, library2, library3, library4)
// FILE: main.kt
package test

class Unrelated

class A {
    fun callable(): Unrelated = Unrelated()
}

// callable: library/A.callable
