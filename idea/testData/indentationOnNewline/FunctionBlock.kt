package testing

private fun <T> times(times : Int, body : () -> T) {}

fun main(args: Array<String>) {
    times(3) {<caret>}
}

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER