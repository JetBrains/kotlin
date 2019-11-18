// IGNORE_BACKEND_FIR: JVM_IR
var invokeOrder: String = ""

fun test(x: Double = { invokeOrder += "x"; 1.0 }(), a: String, y: Long = { invokeOrder += "y"; 1 }(), b: String): String {
    return "" + x.toInt() + a + b + y;
}

fun box(): String {
    val funResult = test(b = { invokeOrder += "K"; "K" }(), a = { invokeOrder += "O"; "O" }())

    if (invokeOrder != "KOxy" || funResult != "1OK1") return "fail: $invokeOrder != KOxy or $funResult != 1OK1"

    return "OK"
}