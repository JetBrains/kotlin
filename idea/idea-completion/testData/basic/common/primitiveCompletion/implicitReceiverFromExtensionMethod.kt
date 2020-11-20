// FIR_COMPARISON
class A {
    fun aaa() {}
    val aa = 10
}

fun A.aaaExt() {}

fun A.test() {
    <caret>
}

// EXIST: aaa
// EXIST: aa
// EXIST: aaaExt
