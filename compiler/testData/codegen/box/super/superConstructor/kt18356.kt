// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val addr: Long, val name: String)

fun box(): String {
    val obj1 = object : Base(name = "OK", addr = 0x1234L) {}
    if (obj1.addr != 0x1234L) return "fail ${obj1.addr}"
    return obj1.name
}

