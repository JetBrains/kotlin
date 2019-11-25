// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val f = fun (s: String): String = s
    val g = f as String.() -> String
    if ("OK".g() != "OK") return "Fail 1"

    val h = fun String.(): String = this
    val i = h as (String) -> String
    if (i("OK") != "OK") return "Fail 2"

    return "OK"
}
