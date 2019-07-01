fun test() {
    when<caret> (val a = 42) {
        else -> use("")
    }
}

fun use(s: String) {}