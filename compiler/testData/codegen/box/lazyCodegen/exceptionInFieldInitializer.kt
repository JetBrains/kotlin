// IGNORE_BACKEND: JVM_IR
class A(val p: String) {
    val prop: String = throw RuntimeException()
}

class B(val p: String) {
    val prop: String = if (p == "test") "OK" else throw RuntimeException()
}

fun box(): String {
    var result = "fail"
    try {
        if (A("test").prop != "OK") return "fail 1"
    }
    catch (e: RuntimeException) {
        result = "OK"
    }
    if (result != "OK") return "fail 1: $result"


    if (B("test").prop != "OK") return "fail 2"


    result = "fail"
    try {
        if (B("fail").prop != "OK") return "fail 3"
    }
    catch (e: RuntimeException) {
        return "OK"
    }

    return "fail"
}