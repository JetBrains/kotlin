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

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: library3.kt
package library

class T1

typealias A = T1

// MODULE: library4
// MODULE_KIND: LibraryBinary
// FILE: library4.kt
package library

class T2

typealias A = T2

// MODULE: main(library1, library2, library3, library4)
// FILE: main.kt
package test

class A {
    val unrelated: Int = 0
}

// class_like: library/A
