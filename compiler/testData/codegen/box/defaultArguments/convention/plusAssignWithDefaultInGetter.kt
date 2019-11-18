// IGNORE_BACKEND_FIR: JVM_IR
class X {
    var result: String = "fail"

    operator fun get(name: String, type: String = "none") = name + type

    operator fun set(name: String, s: String) {
        result = name + s;
    }
}

class Y {
    var result: String = "fail"

    operator fun get(name: String, type: String = "no", type2: String = "ne") = name + type + type2

    operator fun set(name: String, s: String) {
        result = name + s;
    }
}

fun box(): String {
    var x = X()
    x["a"] += "OK"
    if (x.result != "aanoneOK") return "fail: ${x.result}"

    var y = Y()
    y["a"] += "OK"
    if (y.result != "aanoneOK") return "fail: ${y.result}"

    return "OK"
}