// PROBLEM: none
// WITH_RUNTIME
fun test(flag: Boolean): Int {
    return <caret>if (!flag) throw IllegalArgumentException() else 1
}

