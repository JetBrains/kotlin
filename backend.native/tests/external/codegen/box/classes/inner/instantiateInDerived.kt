open class A(val value: String) {
    inner class B(val s: String) {
        val result = value + "_" + s
    }
}

class C : A("fromC") {
    fun classReceiver() = B("OK")
    fun superReceiver() = super.B("OK")

    fun newAReceiver() = A("fromA").B("OK")
    fun aReceiver(): B {
        val a = A("fromA")
        return a.B("OK")
    }

    fun A.extReceiver() = this.B("OK")
    fun extReceiver() = A("fromA").extReceiver()
}

fun box(): String {
    val receiver = C()
    var result = receiver.classReceiver().result
    if (result != "fromC_OK") return "fail 1: $result"

    result = receiver.superReceiver().result
    if (result != "fromC_OK") return "fail 2: $result"


    result = receiver.aReceiver().result
    if (result != "fromA_OK") return "fail 3: $result"

    result = receiver.newAReceiver().result
    if (result != "fromA_OK") return "fail 3: $result"

    result = receiver.extReceiver().result
    if (result != "fromA_OK") return "fail 3: $result"

    return "OK"
}