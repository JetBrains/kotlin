package outer

fun f(c: () -> Unit) {
}

fun <T> f(c: () -> T): String = ""

