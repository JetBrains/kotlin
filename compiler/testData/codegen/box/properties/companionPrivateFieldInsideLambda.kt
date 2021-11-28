fun <T> eval(fn: () -> T) = fn()

class My {
    companion object {
        private val my: String = "O"
            get() = eval { field } + "K"

        fun getValue() = my
    }
}

fun box() = My.getValue()