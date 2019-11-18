// IGNORE_BACKEND_FIR: JVM_IR
class X {
    operator fun get(name: String, type: String = "none") = name + type
}

fun box(): String {
    if (X().get("a") != "anone") return "fail 1: ${X().get("a")}"

    if (X()["a"] != "anone") return "fail 2: ${X()["a"]}"

    return "OK"
}