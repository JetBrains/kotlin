// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
fun test() = foo({ line: String -> line })

fun <T> foo(x: T): T = TODO()

fun box(): String {
    return "OK"
}