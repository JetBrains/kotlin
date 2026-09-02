// WITH_STDLIB

fun test() {
    var x: Int? = 42
    run {
        if (x != null)
            <expr>x.inc()</expr>
    }
    x = getNullableInt()
}

fun getNullableInt(): Int? = 0

fun <R> run(block: () -> R) {
    block()
}
