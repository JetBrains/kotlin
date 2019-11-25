// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    return if (int_invoker( { 7 } ) == 7) "OK" else "fail"
}

fun int_invoker(gen :  () -> Int) : Int {
    return gen()
}
