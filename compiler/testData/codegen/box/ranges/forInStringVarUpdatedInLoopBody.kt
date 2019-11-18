// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var str = "OK"
    var r = ""
    for (ch in str) {
        r += ch
        str = "zzz"
    }
    return r
}