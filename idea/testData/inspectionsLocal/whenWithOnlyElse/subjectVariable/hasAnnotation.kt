fun hasAnnotation() {
    <caret>when (@Bar val a = create()) {
        else -> use(a)
    }
}

fun create(): String = ""

fun use(s: String) {}

annotation class Bar