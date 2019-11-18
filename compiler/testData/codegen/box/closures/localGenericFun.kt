// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {

    fun <T> local(s : T) : T {
        return s;
    }

    if (local(10) != 10) return "fail1"

    if (local("11") != "11") return "fail2"

    return "OK"
}