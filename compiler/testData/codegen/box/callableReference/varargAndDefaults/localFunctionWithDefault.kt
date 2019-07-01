// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR, JS

fun call0(f: (String) -> String, x: String): String = f(x)
fun call1(f: (String, String) -> String, x: String, y: String): String = f(x, y)
fun call2(f: (String, String, String) -> String, x: String, y: String, z: String): String = f(x, y, z)

fun box(): String {

    var s = "1"

    fun foo(x: String, y: String = "5", z: String = "4"): String = s + x + y + z

    val r = call1(::foo, "2", "3")
    if (r != "1234") return "FAIL $r"

    fun bar(x: String, vararg y: CharSequence = arrayOf("2")): String = s + x + y.size + y[0]

    s = "5"
    val r0 = call0(::bar, "3")
    if (r0 != "5312") return "FAIL1 $r0"

    s = "6"
    val r2 = call1(::bar, "2", "5")
    if (r2 != "6215") return "FAIL2 $r2"

    s = "7"
    val r3 = call2(::bar, "8", "9", "10")
    if (r3 != "7829") return "FAIL3 $r3"
    return "OK"
}