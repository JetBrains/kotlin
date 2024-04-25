// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

val x1: (String) -> Unit = run {
    lambda@{ foo ->
        bar(foo)
    }
}

val x2: (String) -> Unit = run {
    ({ foo ->
        bar(foo)
    })
}

val x3: (String) -> Unit = run {
    (lambda@{ foo ->
        bar(foo)
    })
}

val x4: (String) -> Unit = run {
    return@run (lambda@{ foo ->
        bar(foo)
    })
}

fun bar(s: String) {}
fun <R> run(block: () -> R): R = block()
