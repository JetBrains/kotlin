// IGNORE_BACKEND_FIR: JVM_IR
inline fun <reified T> Any?.check(): Boolean {
    return this is T
}

inline fun <reified T> Any?.check2(): Boolean {
    return check<T>()
}


var log = ""
fun log(a: Any?) {
    log += a.toString() + ";"
}

fun test(a: Any?) {
    log(a.check<String>())
    log(a.check<String?>())
}

fun test2(a: Any?) {
    log(a.check2<String>())
    log(a.check2<String?>())
}

fun box(): String {
    test("")
    test(null)
    test2("")
    test2(null)

    if (log != "true;true;false;true;true;true;false;true;") {
        return "fail"
    }

    return "OK"
}
