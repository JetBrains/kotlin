// WITH_RUNTIME
fun test() {
    val x = <caret>when (val a = 42) {
        else -> {
            use("")
        }
    }
}

fun use(s: String) {}