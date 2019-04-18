fun hasAnnotation() {
    when (@Bar val a = create()) {
        else -> use(a)
    }<caret>
}

fun create(): String = ""

fun use(s: String) {}

annotation class Bar