// WITH_STDLIB

fun test() {
    var x: Int? = 42
    if (x != null)
        <expr>x.inc()</expr>
    run {
        x = null
    }
}