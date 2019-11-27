// IGNORE_BACKEND_FIR: JVM_IR

class C() {
    fun Any.toMyPrefixedString(prefix: String = "", suffix: String="") : String = prefix + " " + suffix

    fun testReceiver() : String {
        val res : String = "mama".toMyPrefixedString("111", "222")
        return res
    }

}

fun box() : String {
    if(C().testReceiver() != "111 222") return "fail"
    return "OK"
}
