// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var res = "";
    var call = test(a = {res += "K"; "K"}(), b = {res+="O"; "O"}(), c = {res += "L"; "L"})
    if (res != "KOL" || call != "KOL") return "fail 1: $res != KOL or $call != KOL"

    res = "";
    call = test(a = {res += "K"; "K"}(), c = {res += "L"; "L"}, b = {res+="O"; "O"}())
    if (res != "KOL" || call != "KOL") return "fail 2: $res != KOL or $call != KOL"


    res = "";
    call = test(c = {res += "L"; "L"}, a = {res += "K"; "K"}(), b = {res+="O"; "O"}())
    if (res != "KOL" || call != "KOL") return "fail 3: $res != KOL or $call != KOL"

    return "OK"

}

fun test(a: String, b: String, c: () -> String): String {
    return a + b + c();
}