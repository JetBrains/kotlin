// IGNORE_BACKEND: JVM_IR
var result = "Fail"

fun foo(newResult: String) {
    result = newResult
}

fun box(): String {
    val x = ::foo
    x("OK")
    return result
}
