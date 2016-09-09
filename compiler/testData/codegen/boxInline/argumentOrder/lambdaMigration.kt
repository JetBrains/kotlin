// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun test(a: String, b: String, c: () -> String): String {
    return a + b + c();
}

// FILE: 2.kt

import test.*

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
