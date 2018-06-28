// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val k = object {
        val ok = "OK"
    }

    return k.ok
}
