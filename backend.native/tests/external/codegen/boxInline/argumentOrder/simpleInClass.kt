// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

class Z(val p: String) {
     inline fun test(a: String, b: String, c: () -> String): String {
        return a + b + c() + p;
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var res = "";
    var call = Z("Z").test(b = {res += "K"; "K"}(), a = {res+="O"; "O"}(), c = {res += "L"; "L"})
    if (res != "KOL" || call != "OKLZ") return "fail 1: $res != KOL or $call != OKLZ"

    res = "";
    call = Z("Z").test(b = {res += "K"; "K"}(), c = {res += "L"; "L"}, a = {res+="O"; "O"}())
    if (res != "KOL" || call != "OKLZ") return "fail 2: $res != KOL or $call != OKLZ"


    res = "";
    call = Z("Z").test(c = {res += "L"; "L"}, b = {res += "K"; "K"}(), a = {res+="O"; "O"}())
    if (res != "KOL" || call != "OKLZ") return "fail 3: $res != KOL or $call != OKLZ"

    return "OK"

}
