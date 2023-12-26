// JVM_ABI_K1_K2_DIFF: KT-63984
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
