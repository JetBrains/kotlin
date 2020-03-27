// IGNORE_BACKEND_FIR: JVM_IR
abstract class Your {
    abstract val your: String

    fun foo() = your
}

class My {
    val my: String = "O"
        get() = object : Your() {
            override val your = field
        }.foo() + "K"
}

fun box() = My().my