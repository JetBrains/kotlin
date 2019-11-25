// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {

    fun <T> local(s : T) : T {
        return s;
    }

    fun test(s : Int) : Int {
        return local(s)
    }

    if (test(10) != 10) return "fail1"

    return "OK"
}