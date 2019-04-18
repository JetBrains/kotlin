// WITH_RUNTIME
fun test() {
    val x = when (val a = 42) {
        else -> use("")
    }<caret>
}

fun use(s: String) {}