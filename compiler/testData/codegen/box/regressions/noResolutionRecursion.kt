// IGNORE_BACKEND_FIR: JVM_IR
fun <T> T.at(element: Int) = this.at()

fun <T> T.at(): T = this

fun box(): String = "OK".at()