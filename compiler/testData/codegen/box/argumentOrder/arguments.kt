// IGNORE_BACKEND_FIR: JVM_IR

fun test(a: String, b: String): String {
    return a + b;
}

fun box(): String {
    var res = "";
    val call = test(b = {res += "K"; "K"}(), a = {res+="O"; "O"}())

    if (res != "KO" || call != "OK") return "fail: $res != KO or $call != OK"

    return "OK"
}