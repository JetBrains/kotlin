// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: a1.kt
// BINARY_ROOT: b1
package library

interface L1

class A {
    class Nested : L1
}

class B {
    class Nested
}

// FILE: a2.kt
// BINARY_ROOT: b2
package library

interface L2

class A {
    class Nested : L2
}

object C {
    object Nested
}

// MODULE: main(library)
// FILE: main.kt
package test

class Unrelated

class A {
    class Nested
}

// class: library/A.Nested
