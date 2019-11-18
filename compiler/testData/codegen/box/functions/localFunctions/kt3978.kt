// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    fun local(i: Int = 1) : Int {
        return i
    }

    if (local() != 1) return "Fail 1"
    if (local(2) != 2) return "Fail 2"

    return "OK"
}