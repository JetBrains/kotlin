fun test() {
    <caret>when (val a = create()) {
        else -> use(a)
    }
}

fun create(): String = ""

fun use(s: String) {}