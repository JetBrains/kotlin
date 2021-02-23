package test

class A

class Test {
    fun some(vararg a: <caret>A) = 12
}

// REF: (test).A
