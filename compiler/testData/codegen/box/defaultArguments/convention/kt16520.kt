// LANGUAGE: +ProperArrayConventionSetterWithDefaultCalls
var result = "fail"

class A {
    operator fun set(
        i1: Int,
        i2: Int = 1,
        v: String
    ) {
        result = "" + i1 + i2 + v
    }
}

fun box(): String {
    A()[1] = "OK"
    return if (result != "11OK") "fail: $result" else "OK"
}