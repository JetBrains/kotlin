// TARGET_BACKEND: JVM

var global = ""

fun Runnable(f: () -> Unit) = object : Runnable {
    public override fun run() {
        global = "OK"
    }
}

fun box(): String {
    Runnable { global = "FAIL" } .run()
    return global
}