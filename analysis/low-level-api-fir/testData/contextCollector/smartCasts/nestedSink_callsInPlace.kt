// WITH_STDLIB

fun test() {
    var x: Int? = 42
    x = getNullableInt()
    run {
        if (x != null)
            <expr>x.inc()</expr>
    }
}