// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    with("O") {
        return (context(a: String) fun (y: String): String = a + y)("K")
    }
}