// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val s: String? = "abc"
    val c = s?.get(0)!! - 'b'
    if (c != -1) return "Fail c: $c"

    val d = 'b' - s?.get(2)!!
    if (d != -1) return "Fail d: $d"

    val e = s?.get(2)!! - s?.get(0)!!
    if (e != 2) return "Fail e: $e"

    val f = s?.get(2)!!.minus(s?.get(0)!!)
    if (f != 2) return "Fail f: $f"

    return "OK"
}
