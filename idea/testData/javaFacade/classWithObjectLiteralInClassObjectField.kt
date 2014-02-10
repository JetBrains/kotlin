package test

trait A

class <caret>B {
    class object {
        fun bar() = object : A { }
    }
}
