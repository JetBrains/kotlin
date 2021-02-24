// FIR_COMPARISON
class A {
    fun aa() {}
    val aaa = 10
}

fun A.run(action: A.() -> Unit) {}

fun test() {
    val a = A()
    a.run {
        <caret>
    }
}

// this does not work for some reason
//fun A.test() {
//}

// EXIST: aa
// EXIST: aaa
// EXIST: run