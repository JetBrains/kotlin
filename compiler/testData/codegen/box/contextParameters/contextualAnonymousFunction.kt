// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    val f = context(a: String) fun () = a

    return f("O") + with("K") {
        f()
    }
}