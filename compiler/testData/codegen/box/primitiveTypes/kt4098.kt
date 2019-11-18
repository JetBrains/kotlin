// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val c: Char? = '0'
    c!!.toInt()

    "123456"?.get(0)!!.toInt()

    "123456"!!.get(0).toInt()

    return "OK"
}
