package test

import test.A.O

object A {
    object <caret>O {

    }
}

class B {
    val x = A.O
}