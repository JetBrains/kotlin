package test

class A

class Many {
    class object {
        val x = A()
    }

    class object {
        val y = <caret>A()
    }
}

// REF: (test).A