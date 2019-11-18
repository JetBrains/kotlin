// IGNORE_BACKEND_FIR: JVM_IR
fun foo() = "OK"

fun box(): String {
    val x = ::foo

    var r = x()
    if (r != "OK") return r

    r = run(::foo)
    if (r != "OK") return r

    return "OK"
}
