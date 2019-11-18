// IGNORE_BACKEND_FIR: JVM_IR
private var x = "O"
private fun f() = "K"

fun box() = { x + f() }()
