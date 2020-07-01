// WITH_RUNTIME
fun String.test(): String {
    return if (isBlank<caret>()) {
        "foo"
    } else {
        this
    }
}