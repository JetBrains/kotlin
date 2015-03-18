package test

trait A

class <caret>B {
    companion object {
        fun bar() = object : A { }
    }
}
