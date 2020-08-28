// FIR_COMPARISON
fun test() {
    fun aa() {}
    val aaa = 10

    <caret>
}

// EXIST: aa
// EXIST: aaa