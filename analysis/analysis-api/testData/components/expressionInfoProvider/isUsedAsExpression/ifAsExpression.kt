fun block(fn: () -> String): String = fn()

fun inAndEx(string: String?): String {
    return block {
        val x = if (string == null) {
            <expr>string + "x"</expr>
        } else {
            string
        }
        x // <--
    }
}