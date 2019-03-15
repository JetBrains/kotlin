// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR

fun call(f: (String, String) -> String, x: String, y: String): String = f(x, y)

fun box(): String {

    var s = "1"

    fun foo(x: String, y: String = "5", z: String = "4"): String = s + x + y + z

    val r = call(::foo, "2", "3")
    if (r != "1234") return "FAIL $r"
    return "OK"
}