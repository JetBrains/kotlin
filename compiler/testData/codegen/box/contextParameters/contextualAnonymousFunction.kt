// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

fun box(): String {
    val f = context(a: String) fun () = a

    return f("O") + with("K") {
        f()
    }
}
