// IGNORE_BACKEND_FIR: JVM_IR
class My {
    lateinit var x: String
        private set

    fun init(arg: String, f: (String) -> String) { x = f(arg) }
}

fun box(): String {
    val my = My()
    my.init("O") { it + "K" }
    return my.x
}
