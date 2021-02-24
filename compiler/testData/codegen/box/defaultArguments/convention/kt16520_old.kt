// !LANGUAGE: -ProperArrayConventionSetterWithDefaultCalls
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
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
    return if (result != "10OK") "fail: $result" else "OK"
}