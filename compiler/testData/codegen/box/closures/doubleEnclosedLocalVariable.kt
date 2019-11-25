// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val cl = 39
    return if (sum(200, { val ff = {cl}; ff() }) == 239) "OK" else "FAIL"
}

fun sum(arg:Int, f :  () -> Int) : Int {
    return arg + f()
}
