// IGNORE_BACKEND_FIR: JVM_IR
fun foo(x: String) = x

fun box(): String {
    val x = ::foo
    return x("OK")
}
