// WITH_RUNTIME
fun test() {
    <caret>when (val a = 42) {
        else -> use(a, a)
    }
    val a = 33
}

fun use(i: Int, j: Int) {}