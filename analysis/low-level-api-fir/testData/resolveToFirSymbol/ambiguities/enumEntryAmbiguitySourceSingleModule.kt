// MODULE: main
// FILE: main.kt
package test

enum class A {
    Entry {
        val one: Int = 1
    }
}

enum class A {
    Entry {
        val two: Int = 2
    }
}

enum class B {
    Entry
}

enum class A {
    Entry {
        val three: Int = 3
    }
}

object C

// callable: test/A.Entry
