fun <T> run(block: () -> T): T = block()

fun test(a: Any) {
    run {
        // Should be an error, see KT-44810
        when (a) {
            is String -> 1
        }
    }
}
