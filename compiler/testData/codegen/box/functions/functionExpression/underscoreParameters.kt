// IGNORE_BACKEND_FIR: JVM_IR
fun foo(block: (String, String, String) -> String): String = block("O", "fail", "K")

fun box() = foo(fun(x: String, _: String, y: String) = x + y)
