// IGNORE_BACKEND_FIR: JVM_IR
class C {
    fun calc() : String {
        return "OK"
    }
}

fun box(): String? {
    val c: C? = C()
    val arrayList = arrayOf(c?.calc(), "")
    return arrayList[0]
}
