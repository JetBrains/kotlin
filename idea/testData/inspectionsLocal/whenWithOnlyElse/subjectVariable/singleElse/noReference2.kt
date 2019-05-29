fun test() {
    <caret>when (val a = 42) {
        else -> use("")
    }
}

fun use(s: String) {}