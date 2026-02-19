// MODULE: main
// FILE: main.kt
package test

class A {
    fun callable(): Int = 1

    val callable: Int = 1
}

class A {
    fun callable(): Int = 2

    val callable: Int = 2
}

class B {
    fun callable(): Int = 0

    val callable: Int = 0
}

class A {
    fun callable(): Int = 3

    val callable: Int = 3
}

object C {
    fun callable(): Int = 0

    val callable: Int = 0
}

// callable: test/A.callable
