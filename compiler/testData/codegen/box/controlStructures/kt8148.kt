// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
class A(var value: String)

fun box(): String {
    val a = A("start")

    try {
        test(a)
    } catch(e: RuntimeException) {

    }

    if (a.value != "start, try, finally1, finally2") return "fail: ${a.value}"

    return "OK"
}

fun test(a: A) : String {
    try {
        try {
            a.value += ", try"
            return a.value
        } finally {
            a.value += ", finally1"
        }
    } finally {
        a.value += ", finally2"
        throw RuntimeException("fail")
    }
}
