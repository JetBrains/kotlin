// IGNORE_BACKEND_FIR: JVM_IR
abstract class Your {
    abstract val your: String

    fun foo() = your
}

val my: String = "O"
    get() = field + object: Your() {
        override val your = "K"
            get() = field
    }.foo()

fun box() = my