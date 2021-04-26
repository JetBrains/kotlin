fun <T> eval(fn: () -> T) = fn()

val my: String = "O"
    get() = eval { field } + "K"

fun box() = my
