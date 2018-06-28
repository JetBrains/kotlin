// IGNORE_BACKEND: JVM_IR
val my: String = "O"
    get() = { field }() + "K"

fun box() = my
