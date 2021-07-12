class C(val x: String) {
    fun test(a: String, b: String): String =
        x + a + b
}

fun String.test(a: String, b: String): String =
    this + a + b

fun box(): String {
    var res = ""
    var call = {res += "1"; "x"}().test(b = {res += "2"; "b"}(), a = {res += "3"; "a"}())
    if (res != "123" || call != "xab") return "fail1: $res != 123 or $call != xab"

    res = ""
    call = {res += "1"; C("x")}().test(b = {res += "2"; "b"}(), a = {res += "3"; "a"}())
    if (res != "123" || call != "xab") return "fail2: $res != 123 or $call != xab"
    return "OK"
}
