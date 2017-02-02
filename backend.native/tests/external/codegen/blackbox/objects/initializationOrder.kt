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