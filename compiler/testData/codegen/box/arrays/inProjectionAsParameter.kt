// IGNORE_BACKEND_FIR: JVM_IR
fun test(y: Array<in Array<String>>) {
    y[0] = kotlin.arrayOf("OK")
}

fun box() : String {
    val x : Array<Array<*>> = kotlin.arrayOf(kotlin.arrayOf(1))
    test(x)
    return x[0][0] as String
}