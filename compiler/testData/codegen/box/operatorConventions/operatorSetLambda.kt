// IGNORE_BACKEND_FIR: JVM_IR
// See KT-14999

object Obj {
    var key = ""
    var value = ""

    operator fun set(k: String, v: ((String) -> Unit) -> Unit) {
        key += k
        v { value += it }
    }
}

fun box(): String {
    Obj["O"] = label@{ it("K") }
    return Obj.key + Obj.value
}