var my: String = ""
    get() = field + "K"
    set(arg) {
        field = arg
    }

fun box(): String {
    my = "O"
    return my
}
