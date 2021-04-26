fun <T> eval(fn: () -> T) = fn()

private var x = "O"
private fun f() = "K"

fun box() = eval { x + f() }
