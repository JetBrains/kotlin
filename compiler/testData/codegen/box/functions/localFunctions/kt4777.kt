// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

var result = "Fail"

val p = object : Runnable {
    override fun run() {
        fun <T : Any> T.id() = this

        result = "OK".id()
    }
}

fun box(): String {
    p.run()
    return result
}
