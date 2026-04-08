// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

fun box(): String {
    with("O") {
        return (context(a: String) fun (y: String): String = a + y)("K")
    }
}
