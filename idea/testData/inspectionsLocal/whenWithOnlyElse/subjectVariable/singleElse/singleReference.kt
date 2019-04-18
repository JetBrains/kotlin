fun test() {
    when (val a = create()) {
        else -> use(a)
    }<caret>
}

fun create(): String = ""

fun use(s: String) {}