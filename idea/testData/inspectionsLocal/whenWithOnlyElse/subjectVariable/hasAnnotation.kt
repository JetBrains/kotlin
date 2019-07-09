// WITH_RUNTIME
fun hasAnnotation() {
    when<caret> (@Bar val a = create()) {
        else -> use(a)
    }
}

fun create(): String = ""

fun use(s: String) {}

annotation class Bar