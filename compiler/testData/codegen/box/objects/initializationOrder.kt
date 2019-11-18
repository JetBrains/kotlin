// IGNORE_BACKEND_FIR: JVM_IR
var result = "OK"

class A {
    companion object {
        var z = result

        fun patchResult() {
            result = "fail"
        }
    }
}

fun box(): String {
    A.patchResult()
    return A.z
}