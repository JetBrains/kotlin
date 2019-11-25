// IGNORE_BACKEND_FIR: JVM_IR
abstract class Your {
    abstract val your: String

    fun foo() = your
}

class My {
    private val back = "O"
    val my: String
        get() = object : Your() {
            override val your = back
        }.foo() + "K"
}

fun box() = My().my