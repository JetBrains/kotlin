package test

trait A

class <caret>B {
    default object {
        fun bar() = object : A { }
    }
}
