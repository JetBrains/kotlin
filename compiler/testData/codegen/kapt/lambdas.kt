fun a() = run {
    ""
}

fun <R> run(block: () -> R): R = block()
