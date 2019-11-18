// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var res = "";
    var call = test(b = {res += "K"; "K"}(), a = {res+="O"; "O"}(), c = {res += "L"; "L"})
    if (res != "KOL" || call != "OKL") return "fail 1: $res != KOL or $call != OKL"

    res = "";
    call = test(b = {res += "K"; "K"}(), c = {res += "L"; "L"}, a = {res+="O"; "O"}())
    if (res != "KOL" || call != "OKL") return "fail 2: $res != KOL or $call != OKL"


    res = "";
    call = test(c = {res += "L"; "L"}, b = {res += "K"; "K"}(), a = {res+="O"; "O"}())
    if (res != "KOL" || call != "OKL") return "fail 3: $res != KOL or $call != OKL"

    return "OK"

}

fun test(a: String, b: String, c: () -> String): String {
    return a + b + c();
}