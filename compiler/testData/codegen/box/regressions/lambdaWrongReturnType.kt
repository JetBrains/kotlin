// IGNORE_BACKEND_FIR: JVM_IR
fun test() = foo({ line: String -> line })

fun <T> foo(x: T): T = TODO()

fun box(): String {
    return "OK"
}