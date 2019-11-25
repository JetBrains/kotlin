// IGNORE_BACKEND_FIR: JVM_IR
open class A(val value: String) {
    inner class B(val s: String) {
        val result = value + "_" + s
    }
}

class C : A("fromC") {

    inner class X: A("fromX") {
        fun classReceiver() = B("OK")
        fun superReceiver() = super.B("OK")
        fun superXReceiver() = super@X.B("OK")
        fun superXCastReceiver() = (this@X as A).B("OK")

        fun superCReceiver() = super@C.B("OK")
        fun superCCastReceiver() = (this@C as A).B("OK")
    }
}

fun box(): String {
    val receiver = C().X()
    var result = receiver.classReceiver().result
    if (result != "fromX_OK") return "fail 1: $result"

    result = receiver.superReceiver().result
    if (result != "fromX_OK") return "fail 2: $result"

    result = receiver.superXReceiver().result
    if (result != "fromX_OK") return "fail 3: $result"

    result = receiver.superXCastReceiver().result
    if (result != "fromX_OK") return "fail 4: $result"


    result = receiver.superCReceiver().result
    if (result != "fromC_OK") return "fail 3: $result"

    result = receiver.superCCastReceiver().result
    if (result != "fromC_OK") return "fail 4: $result"

    return "OK"
}