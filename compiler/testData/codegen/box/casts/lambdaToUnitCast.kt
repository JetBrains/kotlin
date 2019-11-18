// IGNORE_BACKEND_FIR: JVM_IR
val foo: () -> Unit = {}

fun box(): String {
    foo() as Unit
    return "OK"
}