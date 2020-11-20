// FIR_COMPARISON
class A {
    fun aa() {}
    val aaa = 10
}

fun test() {
    val a = A()

    a.<caret>
}

// EXIST: aa
// EXIST: aaa