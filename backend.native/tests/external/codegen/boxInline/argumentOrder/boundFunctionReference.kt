// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun test(a: String, b: String, c: () -> String): String {
    return a + b + c();
}

// FILE: 2.kt

import test.*

var res = ""

fun String.id() = this

val receiver: String
    get() {
        res += "L"
        return "L"
    }


fun box(): String {
    res = ""
    var call = test(b = { res += "K"; "K" }(), a = { res += "O"; "O" }(), c = receiver::id)
    if (res != "KOL" || call != "OKL") return "fail 1: $res != KOL or $call != OKL"

    res = ""
    call = test(b = { res += "K"; "K" }(), c = receiver::id, a = { res += "O"; "O" }())
    if (res != "KLO" || call != "OKL") return "fail 2: $res != KLO or $call != OKL"


    res = ""
    call = test(c = receiver::id, b = { res += "K"; "K" }(), a = { res += "O"; "O" }())
    if (res != "LKO" || call != "OKL") return "fail 3: $res != LKO or $call != OKL"

    return "OK"
}

