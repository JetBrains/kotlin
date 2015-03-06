package test

class A

class Many {
    default object {
        val x = A()
    }

    default object {
        val y = <caret>A()
    }
}

// REF: (test).A