// FIR_COMPARISON
class A {
    fun aa() {}
    val aaa = 10

    fun Int.extFun() {}
    fun Int.extVal() {}

    fun test() {
        <caret>
    }
}

// EXIST: aa
// EXIST: aaa
// EXIST: test
// ABSENT: extFun
// ABSENT: extVal