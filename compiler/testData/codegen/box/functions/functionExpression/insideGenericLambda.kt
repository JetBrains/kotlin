// IGNORE_BACKEND_FIR: JVM_IR
fun <T> block(block: () -> T): T = block()
fun foo() {}

fun test(): () -> Unit = block { fun() = foo() }

fun box(): String {
    test()
    return "OK"
}
