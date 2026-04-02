interface A {
    fun foo(): String = "A"
}

interface B {
    fun foo(): String = "B"
}

abstract class <caret>Child : A, B
