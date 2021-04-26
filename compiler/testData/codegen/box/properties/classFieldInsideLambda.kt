fun <T> eval(fn: () -> T) = fn()

class My {
    val my: String = "O"
        get() = eval { field } + "K"
}

fun box() = My().my