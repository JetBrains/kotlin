var inc: String = ""

class X {
    var result: String = "fail"

    operator fun get(name: String, type: String = "none") = name + inc + type

    operator fun set(name: String, s: String) {
        result = name + s;
    }
}

operator fun String.inc(): String {
    inc = this + "1"
    return this + "1"
}

fun box(): String {
    var x = X()
    val res = ++x["a"]
    if (x.result != "aanone1") return "fail 1: ${x.result}"

    if (res != "aanone1none") return "fail 2: ${res}"

    return "OK"
}