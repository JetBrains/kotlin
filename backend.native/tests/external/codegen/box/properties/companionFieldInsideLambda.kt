class My {
    companion object {
        val my: String = "O"
            get() = { field }() + "K"
    }
}

fun box() = My.my