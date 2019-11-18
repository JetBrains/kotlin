// IGNORE_BACKEND_FIR: JVM_IR
class C(val value: String = "C") {

    inner class B(val s: String) {
        val result = value + "_" + s
    }

    fun classReceiver() = B("OK")

    fun newCReceiver() = C("newC").B("OK")
    fun cReceiver(): B {
        val c = C("newC")
        return c.B("OK")
    }

    fun C.extReceiver1() = this.B("OK")
    fun extReceiver() = C("newC").extReceiver1()
}

fun box(): String {
    val receiver = C()
    var result = receiver.classReceiver().result
    if (result != "C_OK") return "fail 1: $result"

    result = receiver.cReceiver().result
    if (result != "newC_OK") return "fail 3: $result"

    result = receiver.newCReceiver().result
    if (result != "newC_OK") return "fail 3: $result"

    result = receiver.extReceiver().result
    if (result != "newC_OK") return "fail 3: $result"

    return "OK"
}