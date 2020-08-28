// FIR_COMPARISON
open class A {
    fun aa() {}
    val aaa = 10

    fun test() {
        <caret>
    }
}

class B : A() {
    fun test() {
        <caret>
    }
}

// EXIST: aa
// EXIST: aaa
