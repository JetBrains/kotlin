// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var res = "";
    var call = Z("Z").test(a = {res += "K"; "K"}(), b = {res+="O"; "O"}(), c = {res += "L"; "L"})
    if (res != "KOL" || call != "KOLZ") return "fail 1: $res != KOL or $call != KOLZ"

    res = "";
    call = Z("Z").test(a = {res += "K"; "K"}(), c = {res += "L"; "L"}, b = {res+="O"; "O"}())
    if (res != "KOL" || call != "KOLZ") return "fail 2: $res != KOL or $call != KOLZ"


    res = "";
    call = Z("Z").test(c = {res += "L"; "L"}, a = {res += "K"; "K"}(), b = {res+="O"; "O"}())
    if (res != "KOL" || call != "KOLZ") return "fail 3: $res != KOL or $call != KOLZ"

    return "OK"

}

class Z(val p: String) {

    fun test(a: String, b: String, c: () -> String): String {
        return a + b + c() + p;
    }

}
