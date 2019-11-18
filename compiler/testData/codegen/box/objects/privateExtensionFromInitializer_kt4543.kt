// IGNORE_BACKEND_FIR: JVM_IR
class A(val result: String)

fun a(body: A.() -> String): String {
    val r = A("OK")
    return r.body()
}

object C {
    private fun A.f() = result

    val g = a {
        f()
    }
}

fun box() = C.g
