// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    return (object { val r = "OK" } ?: null)!!.r
}
