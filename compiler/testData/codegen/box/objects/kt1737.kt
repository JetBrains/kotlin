// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String {
    return object {
        fun foo(): String {
            val f = {}
            object : Runnable {
                public override fun run() {
                    f()
                }
            }
            return "OK"
        }
    }.foo()
}
