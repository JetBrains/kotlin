// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var clist = listOf('1', '2', '3', '4')
    var res1 = ""
    for (ch in clist) {
        res1 += ch
        clist = listOf()
    }

    var s = "1234"
    var res2 = ""
    for (ch in s) {
        res2 += ch
        s = ""
    }

    return if (res1 == res2) "OK" else "'$res1' != '$res2'"
}