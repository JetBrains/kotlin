// FIR_COMPARISON
class B {
    fun bb() {}
    val bbb = 20
}

class A {
    fun aa() {}
    val aaa = 10

    fun test() {
        val b = B()

        b.<caret>
    }
}

// EXIST: bb
// EXIST: bbb
// ABSENT: aa
// ABSENT: aaa