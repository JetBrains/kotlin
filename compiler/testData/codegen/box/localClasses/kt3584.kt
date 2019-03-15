// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val s = "captured";

    class A(val param: String = "OK") {
        val s2 = s + param
    }

    if (A().s2 != "capturedOK") return "fail 1: ${A().s2}"

    if (A("Test").s2 != "capturedTest") return "fail 2: ${A("Test").s2}"

    return "OK"
}