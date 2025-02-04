// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun foo(f: Any): String = (f as Function1<String, String>).invoke("OK")

fun box(): String {
    val x = foo(context(a: String) fun () = a)
    if (x != "OK") return "fail"

    val f = context(a: String) fun () = a

    return with("OK") {
        f()
    }
}
