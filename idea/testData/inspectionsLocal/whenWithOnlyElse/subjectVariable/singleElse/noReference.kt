fun test() {
    when (val a = create()) {
        else -> use("")
    }<caret>
}

fun create(): String = ""

fun use(s: String) {}