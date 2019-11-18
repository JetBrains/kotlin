// IGNORE_BACKEND_FIR: JVM_IR
//KT-4656 Wrong capturing a function literal variable

fun box(): String {
    var foo = { 1 }
    var bar = 1

    val t = { "${foo()} $bar" }
    fun b() = "${foo()} $bar"

    foo = { 2 }
    bar = 2

    if (t() != "2 2") return "fail1"
    if (b() != "2 2") return "fail2"
    return "OK"
}