// IGNORE_BACKEND: JVM_IR
class My {
    val my: String = "O"
        get() = { field }() + "K"
}

fun box() = My().my