// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
val foo: () -> Unit = {}

fun box(): String {
    foo() as Unit
    return "OK"
}