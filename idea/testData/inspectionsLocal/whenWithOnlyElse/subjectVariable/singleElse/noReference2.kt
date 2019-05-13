fun test() {
    when (val a = 42) {
        else -> use("")
    }<caret>
}

fun use(s: String) {}