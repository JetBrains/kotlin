// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    return invoker( {"OK"} )
}

fun invoker(gen :  () -> String) : String {
    return gen()
}
