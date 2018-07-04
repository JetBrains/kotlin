// IGNORE_BACKEND: JVM_IR
fun box(): String {
    var result = "Fail"

    fun changeToOK() { result = "OK" }

    val ok = ::changeToOK
    ok()
    return result
}
