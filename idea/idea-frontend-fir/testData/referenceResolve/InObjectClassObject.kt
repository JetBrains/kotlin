package test

class A

object b {
    companion object {
        val x = <caret>A()
    }
}

// REF: (test).A