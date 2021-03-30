fun <T> run(block: () -> T): T = block()

fun test(a: Any, b: Boolean) {
    run {
        if (b) return@run
        when (a) {
            is String -> 1
        }
    }
}
