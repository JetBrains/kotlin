// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: library1.kt
package library

interface L1

class A {
    class Nested : L1
}

class B {
    class Nested
}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: library2.kt
package library

interface L2

class A {
    class Nested : L2
}

object C {
    object Nested
}

// MODULE: main(library1, library2)
// FILE: main.kt
package test

class Unrelated

class A {
    class Nested
}

// class: library/A.Nested
