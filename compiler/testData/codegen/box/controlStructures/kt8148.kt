// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

class A(var value: String)

fun box(): String {
    val a = A("start")

    try {
        test(a)
    } catch(e: java.lang.RuntimeException) {

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
