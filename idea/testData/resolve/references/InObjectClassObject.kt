package test

class A

object b {
    default object {
        val x = <caret>A()
    }
}

// REF: (test).A