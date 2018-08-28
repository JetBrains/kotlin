// IGNORE_BACKEND: JS_IR
class My {
    lateinit var x: String
        private set

    fun init() { x = "OK" }
}

fun box(): String {
    val my = My()
    my.init()
    return my.x
}
