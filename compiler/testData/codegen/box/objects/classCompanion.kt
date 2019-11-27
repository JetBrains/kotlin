// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

class A {

    companion object {

        val prop = test()

        fun test(): String {
            result += "OK"
            return result
        }
    }
}

fun box(): String {
    if (A.prop != "OK") return "fail ${A.prop}"
    return result
}